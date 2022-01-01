package eye.on.the.money.service.impl;

import eye.on.the.money.dto.in.TransactionQuery;
import eye.on.the.money.dto.out.TransactionDTO;
import eye.on.the.money.model.Currency;
import eye.on.the.money.model.crypto.Payment;
import eye.on.the.money.model.User;
import eye.on.the.money.model.crypto.Coin;
import eye.on.the.money.model.crypto.Transaction;
import eye.on.the.money.repository.CoinRepository;
import eye.on.the.money.repository.CurrencyRepository;
import eye.on.the.money.repository.TransactionRepository;
import eye.on.the.money.service.PaymentService;
import eye.on.the.money.service.TransactionService;
import eye.on.the.money.service.currency.CryptoAPIService;
import eye.on.the.money.service.currency.CurrencyConverter;
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
public class TransactionServiceImpl implements TransactionService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private CurrencyRepository currencyRepository;

    @Autowired
    private CurrencyConverter currencyConverter;

    @Autowired
    private CoinRepository coinRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private CryptoAPIService cryptoAPIService;

    @Override
    public List<TransactionDTO> getTransactionsByUserId(Long userId) {
        return this.transactionRepository.findByUser_IdOrderByTransactionDate(userId).stream().map(this::convertToTransactionDTO).collect(Collectors.toList());
    }

    @Override
    public List<TransactionDTO> getTransactionsByUserIdWConvCurr(Long userId, String currency) {
        List<TransactionDTO> transactions =
                this.transactionRepository.findByUser_IdOrderByTransactionDate(userId).stream().map(this::convertToTransactionDTO).collect(Collectors.toList());
        this.currencyConverter.changeTransactionsCurrency(transactions, currency);
        return transactions;
    }

    @Override
    public List<TransactionDTO> getAllPositions(Long userId, TransactionQuery query) {
        Map<String, TransactionDTO> transactionMap = this.getCalculated(userId, query);
        return (new ArrayList<TransactionDTO>(transactionMap.values())).stream().map(transaction -> {
            transaction.setCurrencyId(query.getCurrency());
            return transaction;
        }).collect(Collectors.toList());
    }

    @Override
    public List<TransactionDTO> getCurrentHoldings(Long userId, TransactionQuery query) {
        Map<String, TransactionDTO> transactionMap = this.getCalculated(userId, query);
        List<TransactionDTO> transactionDTOList = (new ArrayList<TransactionDTO>(transactionMap.values()))
                .stream().filter(i -> (i.getQuantity() > 0)).collect(Collectors.toList());
        this.cryptoAPIService.getLiveValue(transactionDTOList, query.getCurrency());
        this.currencyConverter.changeTransactionsCurrency(transactionDTOList, query.getCurrency());
        return transactionDTOList;
    }

    private TransactionDTO convertToTransactionDTO(Transaction transaction) {
        this.modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.LOOSE);
        return this.modelMapper.map(transaction, TransactionDTO.class);
    }

    @Transactional
    @Override
    public void deleteTransactionById(List<Long> ids) {
        this.transactionRepository.deleteByIdIn(ids);
    }

    @Transactional
    @Override
    public TransactionDTO createTransaction(TransactionDTO transactionDTO, User user) {
        Currency currency = this.currencyRepository.findById(transactionDTO.getCurrencyId()).orElseThrow(NoSuchElementException::new);
        Coin coin = this.coinRepository.findBySymbol(transactionDTO.getSymbol()).orElseThrow(NoSuchElementException::new);
        Payment payment = this.paymentService.createPayment(currency, transactionDTO.getAmount());

        Transaction transaction = Transaction.builder()
                .buySell(transactionDTO.getBuySell())
                .transactionDate(transactionDTO.getTransactionDate())
                .transactionString(transactionDTO.getTransactionString())
                .quantity(transactionDTO.getQuantity())
                .creationDate(new Date())
                .coin(coin)
                .payment(payment)
                .user(user)
                .build();
        transaction = this.transactionRepository.save(transaction);
        return this.convertToTransactionDTO(transaction);
    }

    @Transactional
    @Override
    public TransactionDTO updateTransaction(TransactionDTO transactionDTO, User user) {
        Currency currency = this.currencyRepository.findById(transactionDTO.getCurrencyId()).orElseThrow(NoSuchElementException::new);
        Coin coin = this.coinRepository.findBySymbol(transactionDTO.getSymbol()).orElseThrow(NoSuchElementException::new);
        Transaction transaction = this.transactionRepository.findById(transactionDTO.getTransactionId()).orElseThrow(NoSuchElementException::new);
        Payment payment = transaction.getPayment();

        transaction.setBuySell(transactionDTO.getBuySell());
        transaction.setTransactionString(transactionDTO.getTransactionString());
        transaction.setTransactionDate(transactionDTO.getTransactionDate());
        transaction.setQuantity(transactionDTO.getQuantity());
        transaction.setCoin(coin);
        payment.setAmount(transactionDTO.getAmount());
        payment.setCurrency(currency);

        return this.convertToTransactionDTO(transaction);
    }

    private Map<String, TransactionDTO> getCalculated(Long userId, TransactionQuery query) {
        List<TransactionDTO> transactions = this.transactionRepository.findByUser_IdOrderByTransactionDate(userId).stream().map(this::convertToTransactionDTO).collect(Collectors.toList());
        this.currencyConverter.changeTransactionsCurrency(transactions, query.getCurrency());
        Map<String, TransactionDTO> transactionMap = new HashMap<>();
        for (TransactionDTO t : transactions) {
            if (t.getBuySell().equals("S")) {
                t.negateAmountAndQuantity();
            }
            transactionMap.compute(t.getSymbol(), (key, value) -> (value == null) ? t : value.mergeTransactions(t));
        }
        return transactionMap;
    }

    @Override
    public void getCSV(Long userId, Writer writer) {
        List<TransactionDTO> transactionList =
                this.transactionRepository.findByUser_IdOrderByTransactionDate(userId)
                        .stream()
                        .map(this::convertToTransactionDTO).
                        collect(Collectors.toList());
        try (CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT)) {
            if (!transactionList.isEmpty()) {
                csvPrinter.printRecord("Transaction Id", "Quantity", "Type", "Transaction Date", "Symbol", "Amount", "Currency");
            }
            for (TransactionDTO t : transactionList) {
                csvPrinter.printRecord(t.getTransactionId(), t.getQuantity(),
                        t.getBuySell(), t.getTransactionDate(), t.getSymbol(),
                        t.getAmount(), t.getCurrencyId());
            }
        } catch (IOException e) {
            throw new RuntimeException("fail to crate CSV file: " + e.getMessage());
        }
    }

    @Transactional
    @Override
    public void processCSV(User user, MultipartFile file) {
        try (BufferedReader fileReader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
             CSVParser csvParser = new CSVParser(fileReader, CSVFormat.Builder.create()
                     .setHeader("Transaction Id", "Quantity", "Type", "Transaction Date", "Symbol", "Amount", "Currency")
                     .setSkipHeaderRecord(true)
                     .setDelimiter(",")
                     .setTrim(true)
                     .setIgnoreHeaderCase(true)
                     .build())) {

            Iterable<CSVRecord> csvRecords = csvParser.getRecords();
            for (CSVRecord csvRecord : csvRecords) {
                Date transactionDate = new SimpleDateFormat("yyyy-MM-dd").parse(csvRecord.get("Transaction Date"));

                TransactionDTO transaction = TransactionDTO.builder()
                        .buySell(csvRecord.get("Type"))
                        .transactionDate(transactionDate)
                        .amount(Double.parseDouble(csvRecord.get("Amount")))
                        .quantity(Double.parseDouble(csvRecord.get("Quantity")))
                        .currencyId(csvRecord.get("Currency"))
                        .symbol(csvRecord.get("Symbol"))
                        .build();

                if (!("").equals(csvRecord.get("Transaction Id")) &&
                        this.transactionRepository.findById(Long.parseLong(csvRecord.get("Transaction Id"))).isPresent()) {
                    transaction.setTransactionId(Long.parseLong(csvRecord.get("Transaction Id")));
                    this.updateTransaction(transaction, user);
                } else {
                    transaction.setTransactionId(Long.parseLong(csvRecord.get("Transaction Id")));
                    this.createTransaction(transaction, user);
                }

            }
        } catch (IOException | ParseException e) {
            throw new RuntimeException("fail to parse CSV file: " + e.getMessage());
        }
    }
}
