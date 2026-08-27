package eye.on.the.money.service.crypto;

import com.fasterxml.jackson.databind.JsonNode;
import eye.on.the.money.dto.in.TransactionQuery;
import eye.on.the.money.dto.out.TransactionDTO;
import eye.on.the.money.exception.APIException;
import eye.on.the.money.exception.CSVException;
import eye.on.the.money.model.Currency;
import eye.on.the.money.model.User;
import eye.on.the.money.model.crypto.Coin;
import eye.on.the.money.model.crypto.Transaction;
import eye.on.the.money.repository.crypto.CoinRepository;
import eye.on.the.money.repository.crypto.TransactionRepository;
import eye.on.the.money.repository.forex.CurrencyRepository;
import eye.on.the.money.service.shared.ICSVService;
import eye.on.the.money.service.user.UserService;
import eye.on.the.money.util.DateFormats;
import eye.on.the.money.service.api.CryptoAPIService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.modelmapper.ModelMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.Writer;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService implements ICSVService {

    private final TransactionRepository transactionRepository;
    private final UserService userService;
    private final CurrencyRepository currencyRepository;
    private final CoinRepository coinRepository;
    private final ModelMapper modelMapper;
    private final CryptoAPIService cryptoAPIService;
    public List<TransactionDTO> getTransactionsByUserId(Long userId) {
        return this.transactionRepository.findByUserIdOrderByTransactionDateDesc(userId).stream()
                .map(this::convertToTransactionDTO).collect(Collectors.toList());
    }

    public List<TransactionDTO> getTransactionsBetween(Long userId, LocalDate from, LocalDate to) {
        return this.transactionRepository.findByUserIdAndTransactionDateBetweenOrderByTransactionDate(userId, from, to)
                .stream().map(this::convertToTransactionDTO).collect(Collectors.toList());
    }

    public List<TransactionDTO> getAllPositions(Long userId) {
        Map<String, TransactionDTO> transactionMap = this.getCalculated(userId);
        return new ArrayList<>(transactionMap.values());
    }

    @Cacheable(cacheNames = "holdings-crypto", key = "#userId",
            condition = "#query.currency != null and #query.currency.equalsIgnoreCase('EUR')")
    public List<TransactionDTO> getCurrentHoldings(Long userId, TransactionQuery query) {
        return this.currentHoldings(userId, query);
    }

    @CachePut(cacheNames = "holdings-crypto", key = "#userId",
            condition = "#query.currency != null and #query.currency.equalsIgnoreCase('EUR')")
    public List<TransactionDTO> refreshCurrentHoldings(Long userId, TransactionQuery query) {
        return this.currentHoldings(userId, query);
    }

    private List<TransactionDTO> currentHoldings(Long userId, TransactionQuery query) {
        Map<String, TransactionDTO> transactionMap = this.getCalculated(userId);
        List<TransactionDTO> transactionDTOList = (new ArrayList<>(transactionMap.values()))
                .stream().filter(i -> (i.getQuantity() > 0)).collect(Collectors.toList());
        if (transactionDTOList.isEmpty()) return transactionDTOList;

        String ids = transactionDTOList.stream().map(TransactionDTO::getCoinId).collect(Collectors.joining(","));

        try {
            JsonNode root = this.cryptoAPIService.getLiveValueForCoins(query.getCurrency(), ids);
            transactionDTOList.forEach(transactionDTO -> {
                transactionDTO.setLiveValue(root.path(transactionDTO.getCoinId()).get(query.getCurrency().toLowerCase()).doubleValue() * transactionDTO.getQuantity());
                transactionDTO.setValueDiff(transactionDTO.getLiveValue() - transactionDTO.getAmount());
            });
        } catch (APIException e) {
            log.error("Unable to fetch live coin values, returning holdings without live data", e);
        }

        return transactionDTOList;
    }

    private TransactionDTO convertToTransactionDTO(Transaction transaction) {
        return this.modelMapper.map(transaction, TransactionDTO.class);
    }

    @CacheEvict(cacheNames = "holdings-crypto", key = "#userId")
    @Transactional
    public void deleteTransactionById(Long userId, List<Long> ids) {
        this.transactionRepository.deleteByUserIdAndIdIn(userId, ids);
    }

    @CacheEvict(cacheNames = "holdings-crypto", key = "#userId")
    @Transactional
    public TransactionDTO createTransaction(TransactionDTO transactionDTO, Long userId) {
        Currency currency = this.currencyRepository.findById(transactionDTO.getCurrencyId()).orElseThrow(() -> new NoSuchElementException("Currency not found: " + transactionDTO.getCurrencyId()));
        Coin coin = this.coinRepository.findBySymbol(transactionDTO.getSymbol()).orElseThrow(() -> new NoSuchElementException("Coin not found: " + transactionDTO.getSymbol()));
        User user = this.userService.getReference(userId);

        Transaction transaction = Transaction.builder()
                .buySell(transactionDTO.getBuySell())
                .transactionDate(transactionDTO.getTransactionDate())
                .transactionString(transactionDTO.getTransactionString())
                .quantity(transactionDTO.getQuantity())
                .creationDate(LocalDate.now())
                .coin(coin)
                .amount(transactionDTO.getAmount())
                .currency(currency)
                .user(user)
                .fee(transactionDTO.getFee())
                .build();

        transaction = this.transactionRepository.save(transaction);
        return this.convertToTransactionDTO(transaction);
    }

    @CacheEvict(cacheNames = "holdings-crypto", key = "#userId")
    @Transactional
    public TransactionDTO updateTransaction(TransactionDTO transactionDTO, Long userId) {
        Currency currency = this.currencyRepository.findById(transactionDTO.getCurrencyId()).orElseThrow(() -> new NoSuchElementException("Currency not found: " + transactionDTO.getCurrencyId()));
        Coin coin = this.coinRepository.findBySymbol(transactionDTO.getSymbol()).orElseThrow(() -> new NoSuchElementException("Coin not found: " + transactionDTO.getSymbol()));
        Transaction transaction = this.transactionRepository.findByIdAndUserId(transactionDTO.getId(), userId).orElseThrow(() -> new NoSuchElementException("Transaction not found: " + transactionDTO.getId()));

        transaction.setBuySell(transactionDTO.getBuySell());
        transaction.setTransactionString(transactionDTO.getTransactionString());
        transaction.setTransactionDate(transactionDTO.getTransactionDate());
        transaction.setQuantity(transactionDTO.getQuantity());
        transaction.setCoin(coin);
        transaction.setFee(transactionDTO.getFee());
        transaction.setAmount(transactionDTO.getAmount());
        transaction.setCurrency(currency);

        return this.convertToTransactionDTO(transaction);
    }



    private Map<String, TransactionDTO> getCalculated(Long userId) {
        List<TransactionDTO> transactions = this.transactionRepository.findByUserIdOrderByTransactionDate(userId).stream()
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

    public void getCSV(Long userId, Writer writer) {
        List<TransactionDTO> transactionList =
                this.transactionRepository.findByUserIdOrderByTransactionDate(userId)
                        .stream()
                        .map(this::convertToTransactionDTO)
                        .toList();
        this.printRecords(transactionList, writer);
    }

    @CacheEvict(cacheNames = "holdings-crypto", key = "#userId")
    @Transactional
    public void processCSV(Long userId, MultipartFile file) {
        try (CSVParser csvParser = this.getParser(file,
                new String[]{"Transaction Id", "Quantity", "Type", "Transaction Date", "Symbol", "Amount", "Currency", "Fee"})) {
            for (CSVRecord csvRecord : csvParser) {
                TransactionDTO transaction = TransactionDTO.createFromCSVRecord(csvRecord, DateFormats.YYYY_MM_DD);

                if (transaction.getId() != null &&
                        this.transactionRepository.findByIdAndUserId(transaction.getId(), userId).isPresent()) {
                    this.updateTransaction(transaction, userId);
                } else {
                    transaction.setId(null);
                    this.createTransaction(transaction, userId);
                }
            }
        } catch (IOException | DateTimeParseException | IllegalArgumentException e) {
            log.error("Error while processing CSV", e);
            throw new CSVException("Failed to parse CSV file: " + e.getMessage(), e);
        }
    }
}
