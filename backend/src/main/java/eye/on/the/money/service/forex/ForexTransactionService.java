package eye.on.the.money.service.forex;

import com.fasterxml.jackson.databind.JsonNode;
import eye.on.the.money.dto.out.ForexTransactionDTO;
import eye.on.the.money.exception.APIException;
import eye.on.the.money.model.Currency;
import eye.on.the.money.model.User;
import eye.on.the.money.model.forex.ForexTransaction;
import eye.on.the.money.repository.forex.CurrencyRepository;
import eye.on.the.money.repository.forex.ForexTransactionRepository;
import eye.on.the.money.service.api.EODAPIService;
import eye.on.the.money.service.shared.ICSVService;
import eye.on.the.money.service.user.UserService;
import eye.on.the.money.util.DateFormats;
import eye.on.the.money.util.LiveQuote;
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
public class ForexTransactionService implements ICSVService {

    private final CurrencyRepository currencyRepository;
    private final ForexTransactionRepository forexTransactionRepository;
    private final UserService userService;
    private final ModelMapper modelMapper;
    private final EODAPIService eodAPIService;
    public List<ForexTransactionDTO> getForexTransactionsByUserId(Long userId) {
        return this.forexTransactionRepository.findByUserIdOrderByTransactionDate(userId).stream().map(this::convertToForexTransactionDTO).collect(Collectors.toList());
    }

    public List<ForexTransactionDTO> getForexTransactionsBetween(Long userId, LocalDate from, LocalDate to) {
        return this.forexTransactionRepository.findByUserIdAndTransactionDateBetweenOrderByTransactionDate(userId, from, to)
                .stream().map(this::convertToForexTransactionDTO).collect(Collectors.toList());
    }

    private ForexTransactionDTO convertToForexTransactionDTO(ForexTransaction forexTransaction) {
        return this.modelMapper.map(forexTransaction, ForexTransactionDTO.class);
    }

    @CacheEvict(cacheNames = "holdings-forex", key = "#userId")
    @Transactional
    public void deleteForexTransactionById(Long userId, List<Long> ids) {
        this.forexTransactionRepository.deleteByUserIdAndIdIn(userId, ids);
    }

    @CacheEvict(cacheNames = "holdings-forex", key = "#userId")
    @Transactional
    public ForexTransactionDTO createForexTransaction(ForexTransactionDTO forexTransactionDTO, Long userId) {
        Currency toCurrency = this.currencyRepository.findById(forexTransactionDTO.getToCurrencyId()).orElseThrow(() -> new NoSuchElementException("Currency not found: " + forexTransactionDTO.getToCurrencyId()));
        Currency fromCurrency = this.currencyRepository.findById(forexTransactionDTO.getFromCurrencyId()).orElseThrow(() -> new NoSuchElementException("Currency not found: " + forexTransactionDTO.getFromCurrencyId()));
        User user = this.userService.getReference(userId);

        ForexTransaction forexTransaction = ForexTransaction.builder()
                .buySell(forexTransactionDTO.getBuySell())
                .transactionDate(forexTransactionDTO.getTransactionDate())
                .toCurrency(toCurrency)
                .fromCurrency(fromCurrency)
                .fromAmount(forexTransactionDTO.getFromAmount())
                .toAmount(forexTransactionDTO.getToAmount())
                .changeRate(forexTransactionDTO.getBuySell().equals("B") ? forexTransactionDTO.getFromAmount() / forexTransactionDTO.getToAmount() : forexTransactionDTO.getToAmount() / forexTransactionDTO.getFromAmount())
                .user(user)
                .build();

        forexTransaction = this.forexTransactionRepository.save(forexTransaction);
        return this.convertToForexTransactionDTO(forexTransaction);
    }

    @CacheEvict(cacheNames = "holdings-forex", key = "#userId")
    @Transactional
    public ForexTransactionDTO updateForexTransaction(ForexTransactionDTO forexTransactionDTO, Long userId) {
        Currency toCurrency = this.currencyRepository.findById(forexTransactionDTO.getToCurrencyId()).orElseThrow(() -> new NoSuchElementException("Currency not found: " + forexTransactionDTO.getToCurrencyId()));
        Currency fromCurrency = this.currencyRepository.findById(forexTransactionDTO.getFromCurrencyId()).orElseThrow(() -> new NoSuchElementException("Currency not found: " + forexTransactionDTO.getFromCurrencyId()));
        ForexTransaction forexTransaction = this.forexTransactionRepository.findByIdAndUserId(forexTransactionDTO.getForexTransactionId(), userId).orElseThrow(() -> new NoSuchElementException("Forex transaction not found: " + forexTransactionDTO.getForexTransactionId()));

        forexTransaction.setBuySell(forexTransactionDTO.getBuySell());
        forexTransaction.setTransactionDate(forexTransactionDTO.getTransactionDate());
        forexTransaction.setFromAmount(forexTransactionDTO.getFromAmount());
        forexTransaction.setToAmount(forexTransactionDTO.getToAmount());
        forexTransaction.setToCurrency(toCurrency);
        forexTransaction.setFromCurrency(fromCurrency);
        forexTransaction.setChangeRate(forexTransactionDTO.getBuySell().equals("B") ? forexTransactionDTO.getFromAmount() / forexTransactionDTO.getToAmount() : forexTransactionDTO.getToAmount() / forexTransactionDTO.getFromAmount());

        return this.convertToForexTransactionDTO(forexTransaction);
    }

    @Cacheable(cacheNames = "holdings-forex", key = "#userId")
    public List<ForexTransactionDTO> getAllForexHoldings(Long userId) {
        return this.allForexHoldings(userId);
    }

    @CachePut(cacheNames = "holdings-forex", key = "#userId")
    public List<ForexTransactionDTO> refreshAllForexHoldings(Long userId) {
        return this.allForexHoldings(userId);
    }

    private List<ForexTransactionDTO> allForexHoldings(Long userId) {
        Map<String, ForexTransactionDTO> forexTransactionMap = this.getCalculated(userId);
        List<ForexTransactionDTO> forexTransactions = new ArrayList<>(forexTransactionMap.values());
        if (forexTransactions.isEmpty()) return forexTransactions;

        String joinedList = forexTransactions.stream().map(f -> (f.getToCurrencyId() + f.getFromCurrencyId() + ".FOREX")).collect(Collectors.joining(","));

        try {
            JsonNode responseBody = this.eodAPIService.getLiveForexValue(joinedList);
            for (JsonNode forex : responseBody) {
                Optional<ForexTransactionDTO> forexTransactionDTO = forexTransactions.stream().filter
                        (f -> (f.getToCurrencyId() + f.getFromCurrencyId() + ".FOREX").equals(forex.findValue("code").textValue())).findFirst();
                if (forexTransactionDTO.isEmpty()) continue;
                Optional<LiveQuote.Price> rate = LiveQuote.price(forex);
                if (rate.isEmpty()) {
                    log.warn("No live or previous close for {}, leaving holding without live data",
                            forex.findValue("code").textValue());
                    continue;
                }
                forexTransactionDTO.get().setLiveValue(rate.get().value() * forexTransactionDTO.get().getToAmount());
                forexTransactionDTO.get().setLiveChangeRate(rate.get().value());
                forexTransactionDTO.get().setStalePrice(rate.get().stale());
                forexTransactionDTO.get().setValueDiff(forexTransactionDTO.get().getLiveValue() - forexTransactionDTO.get().getFromAmount());
            }
        } catch (APIException e) {
            log.error("Unable to fetch live forex values, returning holdings without live data", e);
        }

        return forexTransactions;
    }

    private Map<String, ForexTransactionDTO> getCalculated(Long userId) {
        List<ForexTransactionDTO> forexTransactions = this.forexTransactionRepository.findByUserIdOrderByTransactionDate(userId).stream().map(this::convertToForexTransactionDTO).collect(Collectors.toList());
        Map<String, ForexTransactionDTO> forexTransactionMap = new HashMap<>();
        for (ForexTransactionDTO ft : forexTransactions) {
            String symbol = ft.getFromCurrencyId() + ft.getToCurrencyId();
            forexTransactionMap.compute(symbol, (key, value) -> (value == null) ? ft : value.mergeTransactions(ft));
        }
        return forexTransactionMap;
    }

    public void getCSV(Long userId, Writer writer) {
        List<ForexTransactionDTO> forexList =
                this.forexTransactionRepository.findByUserIdOrderByTransactionDate(userId)
                        .stream()
                        .map(this::convertToForexTransactionDTO)
                        .toList();
        this.printRecords(forexList, writer);
    }

    @CacheEvict(cacheNames = "holdings-forex", key = "#userId")
    @Transactional
    public void processCSV(Long userId, MultipartFile file) {
        long lineNumber = 0;
        try (CSVParser csvParser = this.getParser(file,
                new String[]{"Transaction Id", "From Amount", "To Amount", "Type", "Transaction Date", "Change Rate", "From Currency", "To Currency"})) {
            for (CSVRecord csvRecord : csvParser) {
                lineNumber = csvParser.getCurrentLineNumber();
                ForexTransactionDTO transaction = ForexTransactionDTO.createFromCSVRecord(csvRecord, DateFormats.YYYY_MM_DD);

                if (transaction.getForexTransactionId() != null &&
                        this.forexTransactionRepository.findByIdAndUserId(transaction.getForexTransactionId(), userId).isPresent()) {
                    log.trace("Update forex transaction {}", transaction);
                    this.updateForexTransaction(transaction, userId);
                } else {
                    transaction.setForexTransactionId(null);
                    log.trace("Create forex transaction {}", transaction);
                    this.createForexTransaction(transaction, userId);
                }
            }
        } catch (IOException | DateTimeParseException | IllegalArgumentException e) {
            log.error("Error while processing CSV", e);
            throw this.csvParseFailure(lineNumber, e);
        }
    }
}
