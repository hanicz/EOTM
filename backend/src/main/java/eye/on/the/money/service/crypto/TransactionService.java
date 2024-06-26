package eye.on.the.money.service.crypto;

import com.fasterxml.jackson.databind.JsonNode;
import eye.on.the.money.dto.in.TransactionQuery;
import eye.on.the.money.dto.out.TransactionDTO;
import eye.on.the.money.exception.CSVException;
import eye.on.the.money.model.Currency;
import eye.on.the.money.model.User;
import eye.on.the.money.model.crypto.Coin;
import eye.on.the.money.model.crypto.Payment;
import eye.on.the.money.model.crypto.Transaction;
import eye.on.the.money.repository.crypto.CoinRepository;
import eye.on.the.money.repository.crypto.TransactionRepository;
import eye.on.the.money.repository.forex.CurrencyRepository;
import eye.on.the.money.service.shared.ICSVService;
import eye.on.the.money.service.user.UserServiceImpl;
import eye.on.the.money.service.api.CryptoAPIService;
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
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService implements ICSVService {

    private final TransactionRepository transactionRepository;
    private final PaymentService paymentService;
    private final UserServiceImpl userService;
    private final CurrencyRepository currencyRepository;
    private final CoinRepository coinRepository;
    private final ModelMapper modelMapper;
    private final CryptoAPIService cryptoAPIService;

    private final static DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public List<TransactionDTO> getTransactionsByUserId(String userEmail) {
        return this.transactionRepository.findByUserEmailOrderByTransactionDateDesc(userEmail).stream()
                .map(this::convertToTransactionDTO).collect(Collectors.toList());
    }

    public List<TransactionDTO> getAllPositions(String userEmail) {
        Map<String, TransactionDTO> transactionMap = this.getCalculated(userEmail);
        return new ArrayList<>(transactionMap.values());
    }

    public List<TransactionDTO> getCurrentHoldings(String userEmail, TransactionQuery query) {
        Map<String, TransactionDTO> transactionMap = this.getCalculated(userEmail);
        List<TransactionDTO> transactionDTOList = (new ArrayList<>(transactionMap.values()))
                .stream().filter(i -> (i.getQuantity() > 0)).collect(Collectors.toList());
        String ids = transactionDTOList.stream().map(TransactionDTO::getCoinId).collect(Collectors.joining(","));
        JsonNode root = this.cryptoAPIService.getLiveValueForCoins(query.getCurrency(), ids);

        transactionDTOList.forEach(transactionDTO -> {
            transactionDTO.setLiveValue(root.path(transactionDTO.getCoinId()).get(query.getCurrency().toLowerCase()).doubleValue() * transactionDTO.getQuantity());
            transactionDTO.setValueDiff(transactionDTO.getLiveValue() - transactionDTO.getAmount());
        });

        return transactionDTOList;
    }

    private TransactionDTO convertToTransactionDTO(Transaction transaction) {
        return this.modelMapper.map(transaction, TransactionDTO.class);
    }

    @Transactional
    public boolean deleteTransactionById(String userEmail, String ids) {
        List<Long> idList = Stream.of(ids.split(",")).map(s -> Long.parseLong(s.trim())).collect(Collectors.toList());
        return this.transactionRepository.deleteByUserEmailAndIdIn(userEmail, idList) == idList.size();
    }

    @Transactional
    public TransactionDTO createTransaction(TransactionDTO transactionDTO, String userEmail) {
        Currency currency = this.currencyRepository.findById(transactionDTO.getCurrencyId()).orElseThrow(NoSuchElementException::new);
        Coin coin = this.coinRepository.findBySymbol(transactionDTO.getSymbol()).orElseThrow(NoSuchElementException::new);
        Payment payment = this.paymentService.createPayment(currency, transactionDTO.getAmount());
        User user = this.userService.loadUserByEmail(userEmail);

        Transaction transaction = Transaction.builder()
                .buySell(transactionDTO.getBuySell())
                .transactionDate(transactionDTO.getTransactionDate())
                .transactionString(transactionDTO.getTransactionString())
                .quantity(transactionDTO.getQuantity())
                .creationDate(LocalDate.now())
                .coin(coin)
                .payment(payment)
                .user(user)
                .fee(transactionDTO.getFee())
                .build();

        transaction = this.transactionRepository.save(transaction);
        return this.convertToTransactionDTO(transaction);
    }

    @Transactional
    public TransactionDTO updateTransaction(TransactionDTO transactionDTO, String userEmail) {
        Currency currency = this.currencyRepository.findById(transactionDTO.getCurrencyId()).orElseThrow(NoSuchElementException::new);
        Coin coin = this.coinRepository.findBySymbol(transactionDTO.getSymbol()).orElseThrow(NoSuchElementException::new);
        Transaction transaction = this.transactionRepository.findByIdAndUserEmail(transactionDTO.getId(), userEmail).orElseThrow(NoSuchElementException::new);
        Payment payment = transaction.getPayment();

        transaction.setBuySell(transactionDTO.getBuySell());
        transaction.setTransactionString(transactionDTO.getTransactionString());
        transaction.setTransactionDate(transactionDTO.getTransactionDate());
        transaction.setQuantity(transactionDTO.getQuantity());
        transaction.setCoin(coin);
        transaction.setFee(transactionDTO.getFee());
        payment.setAmount(transactionDTO.getAmount());
        payment.setCurrency(currency);

        return this.convertToTransactionDTO(transaction);
    }



    private Map<String, TransactionDTO> getCalculated(String userEmail) {
        List<TransactionDTO> transactions = this.transactionRepository.findByUserEmailOrderByTransactionDate(userEmail).stream()
                .map(this::convertToTransactionDTO).toList();
        Map<String, TransactionDTO> transactionMap = new HashMap<>();
        for (TransactionDTO t : transactions) {
            if (t.getBuySell().equals("S")) {
                t.negateAmountAndQuantity();
            }
            transactionMap.compute(t.getSymbol(), (key, value) -> (value == null) ? t : value.mergeTransactions(t));
        }
        return transactionMap;
    }

    public void getCSV(String userEmail, Writer writer) {
        List<TransactionDTO> transactionList =
                this.transactionRepository.findByUserEmailOrderByTransactionDate(userEmail)
                        .stream()
                        .map(this::convertToTransactionDTO)
                        .toList();
        this.printRecords(transactionList, writer);
    }

    @Transactional
    public void processCSV(String userEmail, MultipartFile file) {
        try (CSVParser csvParser = this.getParser(file,
                new String[]{"Transaction Id", "Quantity", "Type", "Transaction Date", "Symbol", "Amount", "Currency", "Fee"})) {
            for (CSVRecord csvRecord : csvParser) {
                TransactionDTO transaction = TransactionDTO.createFromCSVRecord(csvRecord, FORMATTER);

                if (transaction.getId() != null &&
                        this.transactionRepository.findByIdAndUserEmail(transaction.getId(), userEmail).isPresent()) {
                    this.updateTransaction(transaction, userEmail);
                } else {
                    transaction.setId(null);
                    this.createTransaction(transaction, userEmail);
                }
            }
        } catch (IOException | DateTimeParseException | IllegalArgumentException e) {
            log.error("Error while processing CSV", e);
            throw new CSVException("Failed to parse CSV file: " + e.getMessage(), e);
        }
    }
}
