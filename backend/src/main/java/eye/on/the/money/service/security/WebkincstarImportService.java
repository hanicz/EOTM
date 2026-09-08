package eye.on.the.money.service.security;

import eye.on.the.money.dto.out.ImportResultDTO;
import eye.on.the.money.exception.CSVException;
import eye.on.the.money.model.Currency;
import eye.on.the.money.model.User;
import eye.on.the.money.model.security.Interest;
import eye.on.the.money.model.security.Security;
import eye.on.the.money.model.security.SecurityTransaction;
import eye.on.the.money.repository.forex.CurrencyRepository;
import eye.on.the.money.repository.security.InterestRepository;
import eye.on.the.money.repository.security.SecurityTransactionRepository;
import eye.on.the.money.service.shared.IExcelService;
import eye.on.the.money.service.user.UserService;
import eye.on.the.money.util.DateFormats;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebkincstarImportService implements IExcelService {

    private static final String TYPE = "Tranzakció típusa";
    private static final String INSTRUMENT = "Instrumentum";
    private static final String NOMINAL_VALUE = "Névérték";
    private static final String AMOUNT = "Összeg";
    private static final String CURRENCY = "Devizanem";
    private static final String VALUE_DATE = "Értéknap";

    private static final String INTEREST_PAYMENT = "Esedékesség fizetés";
    private static final String BUY = "Vétel";
    private static final String SELL = "Eladás";
    private static final Set<String> IMPORTED_TYPES = Set.of(INTEREST_PAYMENT, BUY, SELL);

    private final SecurityTransactionRepository securityTransactionRepository;
    private final InterestRepository interestRepository;
    private final CurrencyRepository currencyRepository;
    private final SecurityService securityService;
    private final UserService userService;

    @Transactional
    public ImportResultDTO processXls(Long userId, MultipartFile file) {
        User user = this.userService.getReference(userId);
        Set<Long> claimedTransactions = new HashSet<>();
        Set<Long> claimedInterests = new HashSet<>();
        int created = 0;
        int updated = 0;
        int rowNumber = 0;
        try (Workbook workbook = this.getWorkbook(file)) {
            Sheet sheet = this.getFirstSheet(workbook);
            Map<String, Integer> indexes = this.headerIndexes(sheet.getRow(sheet.getFirstRowNum()));
            Integer typeColumn = this.requiredColumn(indexes, TYPE);
            Integer instrumentColumn = this.requiredColumn(indexes, INSTRUMENT);
            Integer nominalColumn = this.requiredColumn(indexes, NOMINAL_VALUE);
            Integer amountColumn = this.requiredColumn(indexes, AMOUNT);
            Integer currencyColumn = this.requiredColumn(indexes, CURRENCY);
            Integer valueDateColumn = this.requiredColumn(indexes, VALUE_DATE);

            for (int index = sheet.getFirstRowNum() + 1; index <= sheet.getLastRowNum(); index++) {
                Row row = sheet.getRow(index);
                rowNumber = index + 1;
                String type = this.stringValue(row, typeColumn);
                if (this.isBlankRow(row) || !IMPORTED_TYPES.contains(type)) {
                    continue;
                }
                Security security = this.securityService.getOrCreateByName(this.stringValue(row, instrumentColumn));
                Currency currency = this.getCurrency(this.stringValue(row, currencyColumn));
                LocalDate valueDate = LocalDate.parse(this.stringValue(row, valueDateColumn),
                        DateFormats.YYYY_MM_DD_DOTTED_SUFFIX);
                Double amount = this.numericValue(row, amountColumn);

                boolean isNew;
                if (INTEREST_PAYMENT.equals(type)) {
                    isNew = this.upsertInterest(userId, user, security, currency, valueDate, amount, claimedInterests);
                } else {
                    int quantity = this.numericValue(row, nominalColumn).intValue();
                    if (quantity == 0) {
                        continue;
                    }
                    isNew = this.upsertTransaction(userId, user, security, currency, valueDate, amount, quantity,
                            BUY.equals(type) ? "B" : "S", claimedTransactions);
                }
                if (isNew) {
                    created++;
                } else {
                    updated++;
                }
            }
        } catch (IOException | DateTimeParseException | IllegalArgumentException e) {
            throw this.excelParseFailure(rowNumber, e);
        }
        log.debug("Imported webkincstár export, created {}, updated {}", created, updated);
        return ImportResultDTO.builder().created(created).updated(updated).build();
    }

    private boolean upsertTransaction(Long userId, User user, Security security, Currency currency,
                                      LocalDate transactionDate, Double amount, int quantity, String buySell,
                                      Set<Long> claimed) {
        for (SecurityTransaction existing : this.securityTransactionRepository
                .findByUserIdAndSecurity_IdAndTransactionDateAndBuySellAndQuantityAndAmountOrderById(
                        userId, security.getId(), transactionDate, buySell, quantity, amount)) {
            if (claimed.add(existing.getId())) {
                return false;
            }
        }
        this.securityTransactionRepository.save(SecurityTransaction.builder()
                .buySell(buySell)
                .creationDate(LocalDate.now())
                .transactionDate(transactionDate)
                .quantity(quantity)
                .amount(amount)
                .currency(currency)
                .security(security)
                .user(user)
                .build());
        return true;
    }

    private boolean upsertInterest(Long userId, User user, Security security, Currency currency,
                                   LocalDate interestDate, Double amount, Set<Long> claimed) {
        for (Interest existing : this.interestRepository
                .findByUserIdAndSecurity_IdAndInterestDateAndAmountAndCurrency_IdOrderById(
                        userId, security.getId(), interestDate, amount, currency.getId())) {
            if (claimed.add(existing.getId())) {
                return false;
            }
        }
        this.interestRepository.save(Interest.builder()
                .amount(amount)
                .interestDate(interestDate)
                .currency(currency)
                .security(security)
                .user(user)
                .build());
        return true;
    }

    private Currency getCurrency(String currencyId) {
        return this.currencyRepository.findById(currencyId)
                .orElseThrow(() -> new CSVException("Unknown currency: " + currencyId));
    }
}
