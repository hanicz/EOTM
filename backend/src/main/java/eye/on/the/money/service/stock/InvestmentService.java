package eye.on.the.money.service.stock;

import com.fasterxml.jackson.databind.JsonNode;
import eye.on.the.money.dto.out.InvestmentDTO;
import eye.on.the.money.exception.APIException;
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
import eye.on.the.money.util.Lots;
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
    public List<InvestmentDTO> getInvestments(Long userId) {
        return this.investmentRepository.findByUserIdOrderByTransactionDateDesc(userId).stream().map(this::convertToInvestmentDTO).collect(Collectors.toList());
    }

    public List<InvestmentDTO> getInvestmentsBetween(Long userId, LocalDate from, LocalDate to) {
        return this.investmentRepository.findByUserIdAndTransactionDateBetweenOrderByTransactionDate(userId, from, to)
                .stream().map(this::convertToInvestmentDTO).collect(Collectors.toList());
    }

    public List<InvestmentDTO> getInvestmentsByAccountId(Long userId, Long accountId) {
        return this.investmentRepository.findByUserIdAndAccountIdOrderByTransactionDateDesc(userId, accountId)
                .stream().map(this::convertToInvestmentDTO).collect(Collectors.toList());
    }

    @Cacheable(cacheNames = "holdings-stock", key = "#userId")
    public List<InvestmentDTO> getCurrentHoldings(Long userId) {
        return this.currentHoldings(userId);
    }

    @CachePut(cacheNames = "holdings-stock", key = "#userId")
    public List<InvestmentDTO> refreshCurrentHoldings(Long userId) {
        return this.currentHoldings(userId);
    }

    private List<InvestmentDTO> currentHoldings(Long userId) {
        List<InvestmentDTO> investments = this.investmentRepository.findByUserIdOrderByTransactionDate(userId).stream().map(this::convertToInvestmentDTO).toList();
        return this.getLiveDataForInvestments(investments);
    }

    public List<InvestmentDTO> getHoldingsByAccountId(Long userId, Long accountId) {
        List<InvestmentDTO> investments = this.investmentRepository.findByUserIdAndAccountIdOrderByTransactionDateDesc(userId, accountId)
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
                        i.setDayChange(price.get().change() == null ? null : price.get().change() * i.getQuantity());
                        i.setDayChangePercent(price.get().changePercent());
                    });
        }

        return investmentDTOList;
    }

    public List<InvestmentDTO> getAllPositions(Long userId) {
        List<InvestmentDTO> investments = this.investmentRepository.findByUserIdOrderByTransactionDate(userId)
                .stream().map(this::convertToInvestmentDTO).toList();
        Map<String, InvestmentDTO> investmentMap = this.getCalculated(investments);
        return new ArrayList<>(investmentMap.values());
    }

    public List<InvestmentDTO> getPositionsByAccountId(Long userId, Long accountId) {
        List<InvestmentDTO> investments = this.investmentRepository.findByUserIdAndAccountIdOrderByTransactionDateDesc(userId, accountId)
                .stream().map(this::convertToInvestmentDTO).toList();
        Map<String, InvestmentDTO> investmentMap = this.getCalculated(investments);
        return new ArrayList<>(investmentMap.values());
    }

    private Map<String, InvestmentDTO> getCalculated(List<InvestmentDTO> investments) {
        return Lots.aggregate(investments, i -> Ticker.symbol(i.getShortName(), i.getExchange()) + "_" + i.getAccountId());
    }

    private InvestmentDTO convertToInvestmentDTO(Investment investment) {
        return this.modelMapper.map(investment, InvestmentDTO.class);
    }

    @CacheEvict(cacheNames = "holdings-stock", key = "#userId")
    @Transactional
    public InvestmentDTO createInvestment(InvestmentDTO investmentDTO, Long userId) {
        Currency currency = this.currencyRepository.findById(investmentDTO.getCurrencyId()).orElseThrow(() -> new NoSuchElementException("Currency not found: " + investmentDTO.getCurrencyId()));
        Stock stock = this.stockService.getOrCreateStock(investmentDTO.getShortName(), investmentDTO.getExchange(), investmentDTO.getName());
        User user = this.userService.getReference(userId);
        Account account = this.accountService.getAccount(userId, investmentDTO.getAccountId());

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

    @CacheEvict(cacheNames = "holdings-stock", key = "#userId")
    @Transactional
    public InvestmentDTO updateInvestment(InvestmentDTO investmentDTO, Long userId) {
        Currency currency = this.currencyRepository.findById(investmentDTO.getCurrencyId()).orElseThrow(() -> new NoSuchElementException("Currency not found: " + investmentDTO.getCurrencyId()));
        Stock stock = this.stockService.getOrCreateStock(investmentDTO.getShortName(), investmentDTO.getExchange(), investmentDTO.getName());
        Investment investment = this.investmentRepository.findByIdAndUserId(investmentDTO.getInvestmentId(), userId).orElseThrow(() -> new NoSuchElementException("Investment not found: " + investmentDTO.getInvestmentId()));
        Account account = this.accountService.getAccount(userId, investmentDTO.getAccountId());

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

    @CacheEvict(cacheNames = "holdings-stock", key = "#userId")
    @Transactional
    public void deleteInvestmentById(Long userId, List<Long> ids) {
        this.investmentRepository.deleteByUserIdAndIdIn(userId, ids);
    }

    public void getCSV(Long userId, Writer writer) {
        List<InvestmentDTO> investmentList =
                this.investmentRepository.findByUserIdOrderByTransactionDate(userId)
                        .stream()
                        .map(this::convertToInvestmentDTO)
                        .toList();
        this.printRecords(investmentList, writer);
    }

    @CacheEvict(cacheNames = "holdings-stock", key = "#userId")
    @Transactional
    public void processCSV(Long userId, MultipartFile file) {
        long lineNumber = 0;
        try (CSVParser csvParser = this.getParser(file,
                new String[]{"Investment Id", "Quantity", "Type", "Transaction Date", "Short Name", "Exchange", "Amount", "Currency", "Fee", "Account"})) {
            Map<String, Long> accountIdsByName = this.accountService.getAccountIdsByName(userId);

            for (CSVRecord csvRecord : csvParser) {
                lineNumber = csvParser.getCurrentLineNumber();
                InvestmentDTO investment = InvestmentDTO.createFromCSVRecord(csvRecord, DateFormats.YYYY_MM_DD);
                investment.setAccountId(this.resolveAccountId(accountIdsByName, investment.getAccountName()));

                if (investment.getInvestmentId() != null &&
                        this.investmentRepository.findByIdAndUserId(investment.getInvestmentId(), userId).isPresent()) {
                    log.trace("Update investment {}", investment);
                    this.updateInvestment(investment, userId);
                } else {
                    investment.setInvestmentId(null);
                    log.trace("Create investment {}", investment);
                    this.createInvestment(investment, userId);
                }
            }
        } catch (IOException | DateTimeParseException | IllegalArgumentException e) {
            log.error("Error while processing CSV", e);
            throw this.csvParseFailure(lineNumber, e);
        }
    }
}
