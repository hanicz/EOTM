package eye.on.the.money.service.security;

import eye.on.the.money.dto.out.SecurityTransactionDTO;
import eye.on.the.money.exception.CSVException;
import eye.on.the.money.model.Currency;
import eye.on.the.money.model.User;
import eye.on.the.money.model.security.Security;
import eye.on.the.money.model.security.SecurityTransaction;
import eye.on.the.money.repository.forex.CurrencyRepository;
import eye.on.the.money.repository.security.SecurityTransactionRepository;
import eye.on.the.money.service.shared.ICSVService;
import eye.on.the.money.service.user.UserServiceImpl;
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
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
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
    private final UserServiceImpl userService;
    private final ModelMapper modelMapper;
    private final SecurityService securityService;
    public List<SecurityTransactionDTO> getTransactions(String userEmail) {
        return this.securityTransactionRepository.findByUserEmailOrderByTransactionDateDesc(userEmail)
                .stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    public List<SecurityTransactionDTO> getCurrentHoldings(String userEmail) {
        List<SecurityTransactionDTO> transactions = this.securityTransactionRepository.findByUserEmailOrderByTransactionDate(userEmail)
                .stream().map(this::convertToDTO).toList();
        Map<String, SecurityTransactionDTO> holdingsMap = this.getCalculated(transactions);
        return new ArrayList<>(holdingsMap.values()).stream()
                .filter(t -> t.getQuantity() > 0)
                .sorted(Comparator.comparing(SecurityTransactionDTO::getAmount).reversed())
                .collect(Collectors.toList());
    }

    private Map<String, SecurityTransactionDTO> getCalculated(List<SecurityTransactionDTO> transactions) {
        Map<String, SecurityTransactionDTO> transactionMap = new HashMap<>();
        transactions.forEach(t -> {
            if (t.getBuySell().equals("S")) {
                t.negateAmountAndQuantity();
            }
            transactionMap.compute(t.getSecurityId(), (k, value) -> (value == null) ? t : value.mergeInvestments(t));
        });
        return transactionMap;
    }

    private SecurityTransactionDTO convertToDTO(SecurityTransaction transaction) {
        return this.modelMapper.map(transaction, SecurityTransactionDTO.class);
    }

    @Transactional
    public SecurityTransactionDTO createTransaction(SecurityTransactionDTO transactionDTO, String userEmail) {
        Currency currency = this.currencyRepository.findById(transactionDTO.getCurrencyId()).orElseThrow(NoSuchElementException::new);
        Security security = this.securityService.getOrCreateSecurity(transactionDTO.getSecurityId(), transactionDTO.getSecurityName());
        User user = this.userService.loadUserByEmail(userEmail);

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
    public SecurityTransactionDTO updateTransaction(SecurityTransactionDTO transactionDTO, String userEmail) {
        Currency currency = this.currencyRepository.findById(transactionDTO.getCurrencyId()).orElseThrow(NoSuchElementException::new);
        Security security = this.securityService.getOrCreateSecurity(transactionDTO.getSecurityId(), transactionDTO.getSecurityName());
        SecurityTransaction transaction = this.securityTransactionRepository.findByIdAndUserEmail(transactionDTO.getTransactionId(), userEmail).orElseThrow(NoSuchElementException::new);

        transaction.setBuySell(transactionDTO.getBuySell());
        transaction.setTransactionDate(transactionDTO.getTransactionDate());
        transaction.setQuantity(transactionDTO.getQuantity());
        transaction.setSecurity(security);
        transaction.setAmount(transactionDTO.getAmount());
        transaction.setCurrency(currency);

        return this.convertToDTO(transaction);
    }

    @Transactional
    public void deleteTransactionById(String userEmail, List<Long> ids) {
        this.securityTransactionRepository.deleteByUserEmailAndIdIn(userEmail, ids);
    }

    public void getCSV(String userEmail, Writer writer) {
        List<SecurityTransactionDTO> transactionList =
                this.securityTransactionRepository.findByUserEmailOrderByTransactionDate(userEmail)
                        .stream()
                        .map(this::convertToDTO)
                        .toList();
        this.printRecords(transactionList, writer);
    }

    @Transactional
    public void processCSV(String userEmail, MultipartFile file) {
        try (CSVParser csvParser = this.getParser(file,
                new String[]{"Transaction Id", "Quantity", "Type", "Transaction Date", "Security Id", "Security Name", "Amount", "Currency"})) {
            for (CSVRecord csvRecord : csvParser) {
                SecurityTransactionDTO transaction = SecurityTransactionDTO.createFromCSVRecord(csvRecord, DateFormats.YYYY_MM_DD);

                if (transaction.getTransactionId() != null &&
                        this.securityTransactionRepository.findByIdAndUserEmail(transaction.getTransactionId(), userEmail).isPresent()) {
                    log.trace("Update transaction {}", transaction);
                    this.updateTransaction(transaction, userEmail);
                } else {
                    transaction.setTransactionId(null);
                    log.trace("Create transaction {}", transaction);
                    this.createTransaction(transaction, userEmail);
                }
            }
        } catch (IOException | DateTimeParseException e) {
            log.error("Error while processing CSV", e);
            throw new CSVException("Failed to parse CSV file: " + e.getMessage(), e);
        }
    }
}
