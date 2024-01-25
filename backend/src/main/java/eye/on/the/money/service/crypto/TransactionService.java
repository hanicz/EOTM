package eye.on.the.money.service.crypto;

import com.fasterxml.jackson.databind.JsonNode;
import eye.on.the.money.dto.in.TransactionQuery;
import eye.on.the.money.dto.out.TransactionDTO;
import eye.on.the.money.model.Currency;
import eye.on.the.money.model.User;
import eye.on.the.money.model.crypto.Coin;
import eye.on.the.money.model.crypto.Payment;
import eye.on.the.money.model.crypto.Transaction;
import eye.on.the.money.repository.crypto.CoinRepository;
import eye.on.the.money.repository.crypto.TransactionRepository;
import eye.on.the.money.repository.forex.CurrencyRepository;
import eye.on.the.money.service.api.CryptoAPIService;
import eye.on.the.money.service.UserServiceImpl;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final PaymentService paymentService;
    private final UserServiceImpl userService;
    private final CurrencyRepository currencyRepository;
    private final CoinRepository coinRepository;
    private final ModelMapper modelMapper;
    private final CryptoAPIService cryptoAPIService;

    @Autowired
    public TransactionService(TransactionRepository transactionRepository, PaymentService paymentService,
                              UserServiceImpl userService, CurrencyRepository currencyRepository,
                              CoinRepository coinRepository, ModelMapper modelMapper, CryptoAPIService cryptoAPIService) {
        this.transactionRepository = transactionRepository;
        this.paymentService = paymentService;
        this.userService = userService;
        this.currencyRepository = currencyRepository;
        this.coinRepository = coinRepository;
        this.modelMapper = modelMapper;
        this.cryptoAPIService = cryptoAPIService;
    }

    public List<TransactionDTO> getTransactionsByUserId(String userEmail) {
        return this.transactionRepository.findByUserEmailOrderByTransactionDate(userEmail).stream()
                .map(this::convertToTransactionDTO).collect(Collectors.toList());
    }

    public List<TransactionDTO> getAllPositions(String userEmail) {
        Map<String, TransactionDTO> transactionMap = this.getCalculated(userEmail);
        return new ArrayList<>((new ArrayList<>(transactionMap.values())));
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
        this.modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.LOOSE);
        return this.modelMapper.map(transaction, TransactionDTO.class);
    }

    @Transactional
    public void deleteTransactionById(String userEmail, List<Long> ids) {
        this.transactionRepository.deleteByUserEmailAndIdIn(userEmail, ids);
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
                .creationDate(new Date())
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
        Transaction transaction = this.transactionRepository.findByIdAndUserEmail(transactionDTO.getTransactionId(), userEmail).orElseThrow(NoSuchElementException::new);
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
        try (CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT)) {
            if (!transactionList.isEmpty()) {
                csvPrinter.printRecord("Transaction Id", "Quantity", "Type", "Transaction Date", "Symbol", "Amount", "Currency", "Fee");
            }
            for (TransactionDTO t : transactionList) {
                csvPrinter.printRecord(t.getTransactionId(), t.getQuantity(),
                        t.getBuySell(), t.getTransactionDate(), t.getSymbol(),
                        t.getAmount(), t.getCurrencyId(), t.getFee());
            }
        } catch (IOException e) {
            throw new RuntimeException("fail to crate CSV file: " + e.getMessage());
        }
    }

    @Transactional
    public void processCSV(String userEmail, MultipartFile file) {
        try (BufferedReader fileReader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
             CSVParser csvParser = new CSVParser(fileReader, CSVFormat.Builder.create()
                     .setHeader("Transaction Id", "Quantity", "Type", "Transaction Date", "Symbol", "Amount", "Currency", "Fee")
                     .setSkipHeaderRecord(true)
                     .setDelimiter(",")
                     .setTrim(true)
                     .setIgnoreHeaderCase(true)
                     .build())) {

            for (CSVRecord csvRecord : csvParser) {
                String transactionId = csvRecord.get("Transaction Id");
                Date transactionDate = new SimpleDateFormat("yyyy-MM-dd").parse(csvRecord.get("Transaction Date"));

                TransactionDTO transaction = TransactionDTO.builder()
                        .buySell(csvRecord.get("Type"))
                        .transactionDate(transactionDate)
                        .amount(Double.parseDouble(csvRecord.get("Amount")))
                        .quantity(Double.parseDouble(csvRecord.get("Quantity")))
                        .currencyId(csvRecord.get("Currency"))
                        .symbol(csvRecord.get("Symbol"))
                        .fee(Double.parseDouble(csvRecord.get("Fee")))
                        .build();

                if (!transactionId.isEmpty() &&
                        this.transactionRepository.findByIdAndUserEmail(Long.parseLong(transactionId), userEmail).isPresent()) {
                    transaction.setTransactionId(Long.parseLong(transactionId));
                    this.updateTransaction(transaction, userEmail);
                } else {
                    this.createTransaction(transaction, userEmail);
                }
            }
        } catch (IOException | ParseException e) {
            throw new RuntimeException("Failed to parse CSV file: " + e.getMessage());
        }
    }
}
