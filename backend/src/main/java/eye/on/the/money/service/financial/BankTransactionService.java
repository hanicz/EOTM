package eye.on.the.money.service.financial;

import eye.on.the.money.dto.in.BankTransactionEditDTO;
import eye.on.the.money.dto.out.BankTransactionDTO;
import eye.on.the.money.dto.out.ImportResultDTO;
import eye.on.the.money.dto.out.MonthlyCashFlowDTO;
import eye.on.the.money.dto.out.MonthlyIncomeDTO;
import eye.on.the.money.exception.CSVException;
import eye.on.the.money.model.Currency;
import eye.on.the.money.model.User;
import eye.on.the.money.model.financial.BankTransaction;
import eye.on.the.money.repository.financial.BankTransactionRepository;
import eye.on.the.money.repository.forex.CurrencyRepository;
import eye.on.the.money.service.shared.ICSVService;
import eye.on.the.money.service.user.UserService;
import eye.on.the.money.util.DateFormats;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.modelmapper.ModelMapper;
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
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class BankTransactionService implements ICSVService {

    private static final char KH_DELIMITER = '\t';
    private static final Charset KH_FALLBACK_CHARSET = Charset.forName("ISO-8859-2");

    private final BankTransactionRepository bankTransactionRepository;
    private final CurrencyRepository currencyRepository;
    private final BankExclusionRuleService bankExclusionRuleService;
    private final UserService userService;
    private final ModelMapper modelMapper;

    public List<BankTransactionDTO> getTransactions(Long userId) {
        return this.bankTransactionRepository.findByUserIdOrderByBookingDateDesc(userId)
                .stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    public List<MonthlyCashFlowDTO> getMonthlyCashFlow(Long userId) {
        return this.bankTransactionRepository.findMonthlyCashFlow(userId);
    }

    public List<MonthlyCashFlowDTO> getCashFlowBetween(Long userId, LocalDate from, LocalDate to) {
        return this.bankTransactionRepository.findCashFlowBetween(userId, from, to);
    }

    public List<MonthlyIncomeDTO> getMonthlyIncome(Long userId) {
        return this.bankTransactionRepository.findMonthlyIncome(userId);
    }

    @Transactional
    public void setExcluded(Long userId, List<Long> ids, boolean excluded) {
        this.bankTransactionRepository.updateExcludedByUserIdAndIdIn(userId, ids, excluded);
    }

    @Transactional
    public void updateTransaction(Long userId, Long id, BankTransactionEditDTO editDTO) {
        BankTransaction transaction = this.bankTransactionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new NoSuchElementException("Bank transaction not found: " + id));
        transaction.setBookingDate(editDTO.bookingDate());
        transaction.setMemo(editDTO.memo().trim());
        this.bankTransactionRepository.save(transaction);
    }

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

    public void getCSV(Long userId, Writer writer) {
        List<BankTransactionDTO> transactionList = this.bankTransactionRepository.findByUserIdOrderByBookingDate(userId)
                .stream()
                .map(this::convertToDTO)
                .toList();
        this.printRecords(transactionList, writer);
    }

    @Transactional
    public ImportResultDTO processCSV(Long userId, MultipartFile file) {
        User user = this.userService.getReference(userId);
        ExclusionRuleMatcher matcher = this.bankExclusionRuleService.matcherFor(userId);
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
                if (this.upsert(transaction, user, matcher)) {
                    created++;
                } else {
                    updated++;
                }
            }
        } catch (IOException | DateTimeParseException | IllegalArgumentException e) {
            log.error("Error while processing CSV", e);
            throw this.csvParseFailure(lineNumber, e);
        }
        log.debug("Imported bank transactions, created {}, updated {}", created, updated);
        return ImportResultDTO.builder().created(created).updated(updated).build();
    }

    private boolean upsert(BankTransactionDTO transactionDTO, User user, ExclusionRuleMatcher matcher) {
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
                .creationDate(LocalDate.now())
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
        return this.modelMapper.map(transaction, BankTransactionDTO.class);
    }
}
