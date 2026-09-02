package eye.on.the.money.service.security;

import eye.on.the.money.dto.out.SecurityTransactionDTO;
import eye.on.the.money.model.Currency;
import eye.on.the.money.model.User;
import eye.on.the.money.model.security.Security;
import eye.on.the.money.model.security.SecurityRate;
import eye.on.the.money.model.security.SecurityTransaction;
import eye.on.the.money.repository.forex.CurrencyRepository;
import eye.on.the.money.repository.security.SecurityTransactionRepository;
import eye.on.the.money.service.shared.ICSVService;
import eye.on.the.money.service.user.UserService;
import eye.on.the.money.util.DateFormats;
import eye.on.the.money.util.Lots;
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
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class SecurityTransactionService implements ICSVService {

    private final SecurityTransactionRepository securityTransactionRepository;
    private final CurrencyRepository currencyRepository;
    private final UserService userService;
    private final ModelMapper modelMapper;
    private final SecurityService securityService;
    private final SecurityRateService securityRateService;

    public List<SecurityTransactionDTO> getTransactions(Long userId) {
        return this.securityTransactionRepository.findByUserIdOrderByTransactionDateDesc(userId)
                .stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    public List<SecurityTransactionDTO> getTransactionsBetween(Long userId, LocalDate from, LocalDate to) {
        return this.securityTransactionRepository.findByUserIdAndTransactionDateBetweenOrderByTransactionDate(userId, from, to)
                .stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    public List<SecurityTransactionDTO> getCurrentHoldings(Long userId) {
        List<SecurityTransactionDTO> transactions = this.securityTransactionRepository.findByUserIdOrderByTransactionDate(userId)
                .stream().map(this::convertToDTO).toList();
        Map<String, SecurityTransactionDTO> holdingsMap = this.getCalculated(transactions);
        List<SecurityTransactionDTO> holdings = new ArrayList<>(holdingsMap.values()).stream()
                .filter(t -> t.getQuantity() > 0)
                .sorted(Comparator.comparing(SecurityTransactionDTO::getAmount).reversed())
                .collect(Collectors.toList());
        this.enrichWithRates(holdings);
        return holdings;
    }

    private void enrichWithRates(List<SecurityTransactionDTO> holdings) {
        Map<String, String> isinBySecurityId = this.securityService.getIsinBySecurityId();
        Map<String, SecurityRate> ratesByIsin =
                this.securityRateService.getNextPaymentByIsin(isinBySecurityId.values());

        for (SecurityTransactionDTO holding : holdings) {
            String isin = isinBySecurityId.get(holding.getSecurityId());
            if (isin == null) continue;
            SecurityRate rate = ratesByIsin.get(isin);
            if (rate == null) continue;

            holding.setNextPaymentDate(rate.getPaymentDate());
            holding.setZeroCoupon(rate.getZeroCoupon());
            if (rate.getZeroCoupon()) {
                holding.setNextPaymentAmount(holding.getQuantity().doubleValue());
                holding.setRate((holding.getQuantity() / holding.getAmount() - 1.0) * 100.0);
            } else {
                holding.setRate(rate.getRate());
                holding.setNextPaymentAmount(
                        holding.getQuantity() * rate.getRate() / 100.0 * this.securityRateService.periodFraction(rate));
            }
        }
    }

    private Map<String, SecurityTransactionDTO> getCalculated(List<SecurityTransactionDTO> transactions) {
        return Lots.aggregate(transactions, SecurityTransactionDTO::getSecurityId);
    }

    private SecurityTransactionDTO convertToDTO(SecurityTransaction transaction) {
        return this.modelMapper.map(transaction, SecurityTransactionDTO.class);
    }

    @Transactional
    public SecurityTransactionDTO createTransaction(SecurityTransactionDTO transactionDTO, Long userId) {
        Currency currency = this.currencyRepository.findById(transactionDTO.getCurrencyId()).orElseThrow(() -> new NoSuchElementException("Currency not found: " + transactionDTO.getCurrencyId()));
        Security security = this.securityService.getOrCreateSecurity(transactionDTO.getSecurityId(), transactionDTO.getSecurityName());
        User user = this.userService.getReference(userId);

        SecurityTransaction transaction = SecurityTransaction.builder()
                .buySell(transactionDTO.getBuySell())
                .creationDate(LocalDate.now())
                .transactionDate(transactionDTO.getTransactionDate())
                .quantity(transactionDTO.getQuantity())
                .amount(transactionDTO.getAmount())
                .currency(currency)
                .security(security)
                .user(user)
                .build();
        transaction = this.securityTransactionRepository.save(transaction);
        return this.convertToDTO(transaction);
    }

    @Transactional
    public SecurityTransactionDTO updateTransaction(SecurityTransactionDTO transactionDTO, Long userId) {
        Currency currency = this.currencyRepository.findById(transactionDTO.getCurrencyId()).orElseThrow(() -> new NoSuchElementException("Currency not found: " + transactionDTO.getCurrencyId()));
        Security security = this.securityService.getOrCreateSecurity(transactionDTO.getSecurityId(), transactionDTO.getSecurityName());
        SecurityTransaction transaction = this.securityTransactionRepository.findByIdAndUserId(transactionDTO.getTransactionId(), userId).orElseThrow(() -> new NoSuchElementException("Security transaction not found: " + transactionDTO.getTransactionId()));

        transaction.setBuySell(transactionDTO.getBuySell());
        transaction.setTransactionDate(transactionDTO.getTransactionDate());
        transaction.setQuantity(transactionDTO.getQuantity());
        transaction.setSecurity(security);
        transaction.setAmount(transactionDTO.getAmount());
        transaction.setCurrency(currency);

        return this.convertToDTO(transaction);
    }

    @Transactional
    public void deleteTransactionById(Long userId, List<Long> ids) {
        this.securityTransactionRepository.deleteByUserIdAndIdIn(userId, ids);
    }

    public void getCSV(Long userId, Writer writer) {
        List<SecurityTransactionDTO> transactionList =
                this.securityTransactionRepository.findByUserIdOrderByTransactionDate(userId)
                        .stream()
                        .map(this::convertToDTO)
                        .toList();
        this.printRecords(transactionList, writer);
    }

    @Transactional
    public void processCSV(Long userId, MultipartFile file) {
        long lineNumber = 0;
        try (CSVParser csvParser = this.getParser(file,
                new String[]{"Transaction Id", "Quantity", "Type", "Transaction Date", "Security Id", "Security Name", "Amount", "Currency"})) {
            for (CSVRecord csvRecord : csvParser) {
                lineNumber = csvParser.getCurrentLineNumber();
                SecurityTransactionDTO transaction = SecurityTransactionDTO.createFromCSVRecord(csvRecord, DateFormats.YYYY_MM_DD);

                if (transaction.getTransactionId() != null &&
                        this.securityTransactionRepository.findByIdAndUserId(transaction.getTransactionId(), userId).isPresent()) {
                    log.trace("Update transaction {}", transaction);
                    this.updateTransaction(transaction, userId);
                } else {
                    transaction.setTransactionId(null);
                    log.trace("Create transaction {}", transaction);
                    this.createTransaction(transaction, userId);
                }
            }
        } catch (IOException | DateTimeParseException | IllegalArgumentException e) {
            log.error("Error while processing CSV", e);
            throw this.csvParseFailure(lineNumber, e);
        }
    }
}
