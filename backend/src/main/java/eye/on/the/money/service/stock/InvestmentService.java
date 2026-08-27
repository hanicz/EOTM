package eye.on.the.money.service.stock;

import com.fasterxml.jackson.databind.JsonNode;
import eye.on.the.money.dto.out.InvestmentDTO;
import eye.on.the.money.exception.APIException;
import eye.on.the.money.exception.CSVException;
import eye.on.the.money.model.stock.Account;
import eye.on.the.money.model.Currency;
import eye.on.the.money.model.User;
import eye.on.the.money.model.stock.Investment;
import eye.on.the.money.model.stock.Stock;
import eye.on.the.money.repository.forex.CurrencyRepository;
import eye.on.the.money.repository.stock.InvestmentRepository;
import eye.on.the.money.repository.stock.RSUTaxDetailsRepository;
import eye.on.the.money.service.api.EODAPIService;
import eye.on.the.money.service.shared.ICSVService;
import eye.on.the.money.service.user.UserService;
import eye.on.the.money.util.DateFormats;
import eye.on.the.money.util.LiveQuote;
import eye.on.the.money.util.Ticker;
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
@Slf4j
@RequiredArgsConstructor
public class InvestmentService implements ICSVService {

    private final InvestmentRepository investmentRepository;
    private final RSUTaxDetailsRepository rsuTaxDetailsRepository;
    private final CurrencyRepository currencyRepository;
    private final AccountService accountService;
    private final UserService userService;
    private final ModelMapper modelMapper;
    private final EODAPIService eodAPIService;
    private final StockService stockService;
    public List<InvestmentDTO> getInvestments(String userEmail) {
        return this.investmentRepository.findByUserEmailOrderByTransactionDateDesc(userEmail).stream().map(this::convertToInvestmentDTO).collect(Collectors.toList());
    }

    public List<InvestmentDTO> getInvestmentsBetween(String userEmail, LocalDate from, LocalDate to) {
        return this.investmentRepository.findByUserEmailAndTransactionDateBetweenOrderByTransactionDate(userEmail, from, to)
                .stream().map(this::convertToInvestmentDTO).collect(Collectors.toList());
    }

    public List<InvestmentDTO> getInvestmentsByAccountId(String userEmail, Long accountId) {
        return this.investmentRepository.findByUserEmailAndAccountIdOrderByTransactionDateDesc(userEmail, accountId)
                .stream().map(this::convertToInvestmentDTO).collect(Collectors.toList());
    }

    @Cacheable(cacheNames = "holdings-stock", key = "#userEmail")
    public List<InvestmentDTO> getCurrentHoldings(String userEmail) {
        return this.currentHoldings(userEmail);
    }

    @CachePut(cacheNames = "holdings-stock", key = "#userEmail")
    public List<InvestmentDTO> refreshCurrentHoldings(String userEmail) {
        return this.currentHoldings(userEmail);
    }

    private List<InvestmentDTO> currentHoldings(String userEmail) {
        List<InvestmentDTO> investments = this.investmentRepository.findByUserEmailOrderByTransactionDate(userEmail).stream().map(this::convertToInvestmentDTO).toList();
        return this.getLiveDataForInvestments(investments);
    }

    public List<InvestmentDTO> getHoldingsByAccountId(String userEmail, Long accountId) {
        List<InvestmentDTO> investments = this.investmentRepository.findByUserEmailAndAccountIdOrderByTransactionDateDesc(userEmail, accountId)
                .stream().map(this::convertToInvestmentDTO).toList();
        return this.getLiveDataForInvestments(investments);
    }

    private List<InvestmentDTO> getLiveDataForInvestments(List<InvestmentDTO> investments) {
        Map<String, InvestmentDTO> investmentMap = this.getCalculated(investments);
        List<InvestmentDTO> investmentDTOList = (new ArrayList<>(investmentMap.values()))
                .stream().filter(i -> (i.getQuantity() > 0)).collect(Collectors.toList());
        if (investmentDTOList.isEmpty()) return investmentDTOList;

        String joinedList = investmentDTOList.stream().map(i -> Ticker.symbol(i.getShortName(), i.getExchange())).distinct().collect(Collectors.joining(","));

        JsonNode responseBody;
        try {
            responseBody = this.eodAPIService.getLiveStockValue(joinedList);
        } catch (APIException e) {
            log.error("Unable to fetch live stock values, returning holdings without live data", e);
            return investmentDTOList;
        }

        for (JsonNode stock : responseBody) {
            String code = stock.findValue("code").textValue();
            Optional<LiveQuote.Price> price = LiveQuote.price(stock);
            if (price.isEmpty()) {
                log.warn("No live or previous close for {}, leaving holding without live data", code);
                continue;
            }
            investmentDTOList.stream()
                    .filter(i -> Ticker.symbol(i.getShortName(), i.getExchange()).equals(code))
                    .forEach(i -> {
                        i.setLiveValue(price.get().value() * i.getQuantity());
                        i.setValueDiff(i.getLiveValue() - i.getAmount());
                        i.setStalePrice(price.get().stale());
                    });
        }

        return investmentDTOList;
    }

    public List<InvestmentDTO> getAllPositions(String userEmail) {
        List<InvestmentDTO> investments = this.investmentRepository.findByUserEmailOrderByTransactionDate(userEmail)
                .stream().map(this::convertToInvestmentDTO).toList();
        Map<String, InvestmentDTO> investmentMap = this.getCalculated(investments);
        return new ArrayList<>(investmentMap.values());
    }

    public List<InvestmentDTO> getPositionsByAccountId(String userEmail, Long accountId) {
        List<InvestmentDTO> investments = this.investmentRepository.findByUserEmailAndAccountIdOrderByTransactionDateDesc(userEmail, accountId)
                .stream().map(this::convertToInvestmentDTO).toList();
        Map<String, InvestmentDTO> investmentMap = this.getCalculated(investments);
        return new ArrayList<>(investmentMap.values());
    }

    private Map<String, InvestmentDTO> getCalculated(List<InvestmentDTO> investments) {
        Map<String, InvestmentDTO> investmentMap = new LinkedHashMap<>();
        Map<String, Integer> lotIndexByKey = new HashMap<>();

        investments.stream()
                .sorted(Comparator.comparing(InvestmentDTO::getTransactionDate))
                .forEach(i -> {
                    if (i.getBuySell().equals("S")) {
                        i.negateAmountAndQuantity();
                    }
                    String baseKey = Ticker.symbol(i.getShortName(), i.getExchange()) + "_" + i.getAccountId();
                    int lotIndex = lotIndexByKey.getOrDefault(baseKey, 0);
                    String key = baseKey + "_" + lotIndex;

                    InvestmentDTO merged = investmentMap.compute(key, (k, value) -> (value == null) ? i : value.mergeInvestments(i));

                    // Position fully closed: seal this lot and start a fresh cost basis for any later re-buy.
                    if (merged.getQuantity() == 0) {
                        lotIndexByKey.put(baseKey, lotIndex + 1);
                    }
                });
        return investmentMap;
    }

    private InvestmentDTO convertToInvestmentDTO(Investment investment) {
        return this.modelMapper.map(investment, InvestmentDTO.class);
    }

    @CacheEvict(cacheNames = "holdings-stock", key = "#userEmail")
    @Transactional
    public InvestmentDTO createInvestment(InvestmentDTO investmentDTO, String userEmail) {
        Currency currency = this.currencyRepository.findById(investmentDTO.getCurrencyId()).orElseThrow(() -> new NoSuchElementException("Currency not found: " + investmentDTO.getCurrencyId()));
        Stock stock = this.stockService.getOrCreateStock(investmentDTO.getShortName(), investmentDTO.getExchange(), investmentDTO.getName());
        User user = this.userService.loadUserByEmail(userEmail);
        Account account = this.accountService.getAccount(userEmail, investmentDTO.getAccountId());

        Investment investment = Investment.builder()
                .buySell(investmentDTO.getBuySell())
                .creationDate(LocalDate.now())
                .transactionDate(investmentDTO.getTransactionDate())
                .user(user)
                .quantity(investmentDTO.getQuantity())
                .stock(stock)
                .amount(investmentDTO.getAmount())
                .currency(currency)
                .fee(investmentDTO.getFee())
                .account(account)
                .build();
        investment = this.investmentRepository.save(investment);
        return this.convertToInvestmentDTO(investment);
    }

    @CacheEvict(cacheNames = "holdings-stock", key = "#userEmail")
    @Transactional
    public InvestmentDTO updateInvestment(InvestmentDTO investmentDTO, String userEmail) {
        Currency currency = this.currencyRepository.findById(investmentDTO.getCurrencyId()).orElseThrow(() -> new NoSuchElementException("Currency not found: " + investmentDTO.getCurrencyId()));
        Stock stock = this.stockService.getOrCreateStock(investmentDTO.getShortName(), investmentDTO.getExchange(), investmentDTO.getName());
        Investment investment = this.investmentRepository.findByIdAndUserEmail(investmentDTO.getInvestmentId(), userEmail).orElseThrow(() -> new NoSuchElementException("Investment not found: " + investmentDTO.getInvestmentId()));
        Account account = this.accountService.getAccount(userEmail, investmentDTO.getAccountId());

        if (investment.isRsu() && this.rsuValuationChanged(investment, investmentDTO, stock)) {
            investment.setRsu(false);
            this.rsuTaxDetailsRepository.deleteByInvestmentIdIn(List.of(investment.getId()));
        }

        investment.setBuySell(investmentDTO.getBuySell());
        investment.setTransactionDate(investmentDTO.getTransactionDate());
        investment.setQuantity(investmentDTO.getQuantity());
        investment.setStock(stock);
        investment.setAccount(account);
        investment.setFee(investmentDTO.getFee());
        investment.setAmount(investmentDTO.getAmount());
        investment.setCurrency(currency);

        return this.convertToInvestmentDTO(investment);
    }

    private boolean rsuValuationChanged(Investment investment, InvestmentDTO investmentDTO, Stock stock) {
        return !Objects.equals(investment.getTransactionDate(), investmentDTO.getTransactionDate())
                || !Objects.equals(investment.getQuantity(), investmentDTO.getQuantity())
                || !Objects.equals(investment.getStock().getId(), stock.getId())
                || !"B".equals(investmentDTO.getBuySell());
    }

    @CacheEvict(cacheNames = "holdings-stock", key = "#userEmail")
    @Transactional
    public void deleteInvestmentById(String userEmail, List<Long> ids) {
        this.investmentRepository.deleteByUserEmailAndIdIn(userEmail, ids);
    }

    public void getCSV(String userEmail, Writer writer) {
        List<InvestmentDTO> investmentList =
                this.investmentRepository.findByUserEmailOrderByTransactionDate(userEmail)
                        .stream()
                        .map(this::convertToInvestmentDTO)
                        .toList();
        this.printRecords(investmentList, writer);
    }

    @CacheEvict(cacheNames = "holdings-stock", key = "#userEmail")
    @Transactional
    public void processCSV(String userEmail, MultipartFile file) {
        try (CSVParser csvParser = this.getParser(file,
                new String[]{"Investment Id", "Quantity", "Type", "Transaction Date", "Short Name", "Exchange", "Amount", "Currency", "Fee", "Account"})) {
            Map<String, Long> accountIdsByName = this.accountService.getAccountIdsByName(userEmail);

            for (CSVRecord csvRecord : csvParser) {
                InvestmentDTO investment = InvestmentDTO.createFromCSVRecord(csvRecord, DateFormats.YYYY_MM_DD);
                investment.setAccountId(this.resolveAccountId(accountIdsByName, investment.getAccountName()));

                if (investment.getInvestmentId() != null &&
                        this.investmentRepository.findByIdAndUserEmail(investment.getInvestmentId(), userEmail).isPresent()) {
                    log.trace("Update investment {}", investment);
                    this.updateInvestment(investment, userEmail);
                } else {
                    investment.setInvestmentId(null);
                    log.trace("Create investment {}", investment);
                    this.createInvestment(investment, userEmail);
                }
            }
        } catch (IOException | DateTimeParseException | IllegalArgumentException e) {
            log.error("Error while processing CSV", e);
            throw new CSVException("Failed to parse CSV file: " + e.getMessage(), e);
        }
    }
}
