package eye.on.the.money.service.financial;

import eye.on.the.money.dto.in.BankTransactionEditDTO;
import eye.on.the.money.dto.in.BankTransactionQuery;
import eye.on.the.money.dto.out.BankTransactionDTO;
import eye.on.the.money.dto.out.ImportResultDTO;
import eye.on.the.money.dto.out.MonthlyCashFlowDTO;
import eye.on.the.money.dto.out.MonthlyCategorySpendingDTO;
import eye.on.the.money.dto.out.MonthlyIncomeDTO;
import eye.on.the.money.dto.out.PageDTO;
import eye.on.the.money.dto.out.YearlyCashFlowDTO;
import eye.on.the.money.exception.CSVException;
import eye.on.the.money.model.Currency;
import eye.on.the.money.model.User;
import eye.on.the.money.model.financial.BankTransaction;
import eye.on.the.money.model.financial.SpendingCategory;
import eye.on.the.money.repository.financial.BankTransactionRepository;
import eye.on.the.money.repository.financial.BankTransactionSpecifications;
import eye.on.the.money.repository.financial.SpendingCategoryRepository;
import eye.on.the.money.repository.forex.CurrencyRepository;
import eye.on.the.money.service.shared.ICSVService;
import eye.on.the.money.service.user.UserService;
import eye.on.the.money.util.DateFormats;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.modelmapper.ModelMapper;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class BankTransactionService implements ICSVService {

    private static final char KH_DELIMITER = '\t';
    private static final Charset KH_FALLBACK_CHARSET = Charset.forName("ISO-8859-2");
    private static final Set<String> SORTABLE_FIELDS = Set.of("bookingDate", "type", "amount");
    private static final String DEFAULT_SORT = "bookingDate";
    private static final int MAX_PAGE_SIZE = 100;

    private final BankTransactionRepository bankTransactionRepository;
    private final CurrencyRepository currencyRepository;
    private final BankExclusionRuleService bankExclusionRuleService;
    private final BankCategoryRuleService bankCategoryRuleService;
    private final SpendingCategoryRepository spendingCategoryRepository;
    private final UserService userService;
    private final ModelMapper modelMapper;

    public PageDTO<BankTransactionDTO> getTransactions(Long userId, BankTransactionQuery query,
                                                       int page, int size, String sort, String direction) {
        return PageDTO.of(this.bankTransactionRepository
                .findAll(BankTransactionSpecifications.matching(userId, query), this.pageRequest(page, size, sort, direction))
                .map(this::convertToDTO));
    }

    private PageRequest pageRequest(int page, int size, String sort, String direction) {
        String field = SORTABLE_FIELDS.contains(sort) ? sort : DEFAULT_SORT;
        Sort.Direction order = Sort.Direction.fromOptionalString(direction).orElse(Sort.Direction.DESC);
        return PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, MAX_PAGE_SIZE),
                Sort.by(order, field).and(Sort.by(Sort.Direction.DESC, "id")));
    }

    @Cacheable(cacheNames = BankReportCaches.MONTHLY, key = "#userId")
    public List<MonthlyCashFlowDTO> getMonthlyCashFlow(Long userId) {
        return this.bankTransactionRepository.findMonthlyCashFlow(userId);
    }

    public List<MonthlyCashFlowDTO> getCashFlowBetween(Long userId, LocalDate from, LocalDate to) {
        return this.bankTransactionRepository.findCashFlowBetween(userId, from, to);
    }

    @Cacheable(cacheNames = BankReportCaches.INCOME, key = "#userId")
    public List<MonthlyIncomeDTO> getMonthlyIncome(Long userId) {
        return this.bankTransactionRepository.findMonthlyIncome(userId);
    }

    @Cacheable(cacheNames = BankReportCaches.CATEGORY, key = "#userId")
    public List<MonthlyCategorySpendingDTO> getMonthlyCategorySpending(Long userId) {
        return this.bankTransactionRepository.findMonthlyCategorySpending(userId);
    }

    @Cacheable(cacheNames = BankReportCaches.YEARLY, key = "#userId")
    public List<YearlyCashFlowDTO> getYearlyCashFlow(Long userId) {
        Map<List<Object>, YearlyCashFlowDTO> years = new LinkedHashMap<>();
        for (MonthlyCashFlowDTO month : this.getMonthlyCashFlow(userId)) {
            years.computeIfAbsent(List.of(month.getYear(), month.getCurrencyId()),
                    key -> YearlyCashFlowDTO.empty(month.getYear(), month.getCurrencyId())).add(month);
        }
        return years.values().stream()
                .sorted(Comparator.comparing(YearlyCashFlowDTO::getYear).reversed()
                        .thenComparing(YearlyCashFlowDTO::getCurrencyId))
                .toList();
    }

    @EvictBankReports
    @Transactional
    public void setExcluded(Long userId, List<Long> ids, boolean excluded) {
        this.bankTransactionRepository.updateExcludedByUserIdAndIdIn(userId, ids, excluded);
    }

    @EvictBankReports
    @Transactional
    public void setCategory(Long userId, List<Long> ids, Long categoryId) {
        SpendingCategory category = categoryId == null ? null
                : this.spendingCategoryRepository.findByIdAndUserId(categoryId, userId)
                .orElseThrow(() -> new NoSuchElementException("Spending category not found: " + categoryId));
        this.bankTransactionRepository.updateCategoryByUserIdAndIdIn(userId, ids, category);
    }

    @EvictBankReports
    @Transactional
    public void updateTransaction(Long userId, Long id, BankTransactionEditDTO editDTO) {
        BankTransaction transaction = this.bankTransactionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new NoSuchElementException("Bank transaction not found: " + id));
        transaction.setBookingDate(editDTO.bookingDate());
        transaction.setMemo(editDTO.memo().trim());
        this.bankTransactionRepository.save(transaction);
    }

    @EvictBankReports
    @Transactional
    public void deleteTransactionById(Long userId, List<Long> ids) {
        this.bankTransactionRepository.deleteByUserIdAndIdIn(userId, ids);
    }

    public void getMonthlyCashFlowCSV(Long userId, Writer writer) {
        this.printRecords(this.getMonthlyCashFlow(userId), writer);
    }

    public void getMonthlyIncomeCSV(Long userId, Writer writer) {
        this.printRecords(this.getMonthlyIncome(userId), writer);
    }

    public void getMonthlyCategorySpendingCSV(Long userId, Writer writer) {
        this.printRecords(this.getMonthlyCategorySpending(userId), writer);
    }

    public void getYearlyCashFlowCSV(Long userId, Writer writer) {
        this.printRecords(this.getYearlyCashFlow(userId), writer);
    }

    public void getCSV(Long userId, Writer writer) {
        List<BankTransactionDTO> transactionList = this.bankTransactionRepository.findByUserIdOrderByBookingDate(userId)
                .stream()
                .map(this::convertToDTO)
                .toList();
        this.printRecords(transactionList, writer);
    }

    @EvictBankReports
    @Transactional
    public ImportResultDTO processCSV(Long userId, MultipartFile file) {
        User user = this.userService.getReference(userId);
        ExclusionRuleMatcher matcher = this.bankExclusionRuleService.matcherFor(userId);
        CategoryRuleMatcher categoryMatcher = this.bankCategoryRuleService.matcherFor(userId);
        int created = 0;
        int updated = 0;
        long lineNumber = 0;
        try (CSVParser csvParser = this.getParser(file, BankTransactionDTO.KH_HEADERS, KH_DELIMITER, this.detectCharset(file))) {
            for (CSVRecord csvRecord : csvParser) {
                lineNumber = csvParser.getCurrentLineNumber();
                if (this.isBlankRecord(csvRecord)) {
                    continue;
                }
                BankTransactionDTO transaction = BankTransactionDTO.createFromKHRecord(csvRecord, DateFormats.YYYY_MM_DD_DOTTED);
                if (this.upsert(transaction, user, matcher, categoryMatcher)) {
                    created++;
                } else {
                    updated++;
                }
            }
        } catch (IOException | DateTimeParseException | IllegalArgumentException e) {
            throw this.csvParseFailure(lineNumber, e);
        }
        log.debug("Imported bank transactions, created {}, updated {}", created, updated);
        return ImportResultDTO.builder().created(created).updated(updated).build();
    }

    private boolean upsert(BankTransactionDTO transactionDTO, User user, ExclusionRuleMatcher matcher,
                           CategoryRuleMatcher categoryMatcher) {
        Currency currency = this.currencyRepository.findById(transactionDTO.getCurrencyId())
                .orElseThrow(() -> new CSVException("Unknown currency: " + transactionDTO.getCurrencyId()));

        Optional<BankTransaction> existing = this.bankTransactionRepository
                .findByUserIdAndBankTransactionIdAndBookingDateAndTypeAndAmountAndMemo(
                        user.getId(), transactionDTO.getBankTransactionId(), transactionDTO.getBookingDate(),
                        transactionDTO.getType(), transactionDTO.getAmount(), transactionDTO.getMemo());

        if (existing.isPresent()) {
            BankTransaction transaction = existing.get();
            transaction.setAccountNumber(transactionDTO.getAccountNumber());
            transaction.setAccountName(transactionDTO.getAccountName());
            transaction.setPartnerAccount(transactionDTO.getPartnerAccount());
            transaction.setPartnerName(transactionDTO.getPartnerName());
            transaction.setCurrency(currency);
            return false;
        }

        this.bankTransactionRepository.save(BankTransaction.builder()
                .bankTransactionId(transactionDTO.getBankTransactionId())
                .bookingDate(transactionDTO.getBookingDate())
                .type(transactionDTO.getType())
                .accountNumber(transactionDTO.getAccountNumber())
                .accountName(transactionDTO.getAccountName())
                .partnerAccount(transactionDTO.getPartnerAccount())
                .partnerName(transactionDTO.getPartnerName())
                .amount(transactionDTO.getAmount())
                .memo(transactionDTO.getMemo())
                .excluded(matcher.matches(transactionDTO.getAccountNumber(), transactionDTO.getPartnerAccount()))
                .category(categoryMatcher.match(transactionDTO.getPartnerName()))
                .currency(currency)
                .user(user)
                .build());
        return true;
    }

    private boolean isBlankRecord(CSVRecord csvRecord) {
        for (String value : csvRecord) {
            if (value != null && !value.isBlank()) {
                return false;
            }
        }
        return true;
    }

    private Charset detectCharset(MultipartFile file) throws IOException {
        CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        try {
            decoder.decode(ByteBuffer.wrap(file.getBytes()));
            return StandardCharsets.UTF_8;
        } catch (CharacterCodingException e) {
            return KH_FALLBACK_CHARSET;
        }
    }

    private BankTransactionDTO convertToDTO(BankTransaction transaction) {
        BankTransactionDTO dto = this.modelMapper.map(transaction, BankTransactionDTO.class);
        SpendingCategory category = transaction.getCategory();
        dto.setCategoryId(category == null ? null : category.getId());
        dto.setCategoryName(category == null ? null : category.getName());
        dto.setCategoryColor(category == null ? null : category.getColor());
        return dto;
    }
}
