package eye.on.the.money.service.etf;

import com.fasterxml.jackson.databind.JsonNode;
import eye.on.the.money.dto.out.ETFInvestmentDTO;
import eye.on.the.money.exception.APIException;
import eye.on.the.money.model.Currency;
import eye.on.the.money.model.User;
import eye.on.the.money.model.etf.ETF;
import eye.on.the.money.model.etf.ETFInvestment;
import eye.on.the.money.model.stock.Account;
import eye.on.the.money.repository.etf.ETFInvestmentRepository;
import eye.on.the.money.repository.forex.CurrencyRepository;
import eye.on.the.money.service.api.EODAPIService;
import eye.on.the.money.service.shared.ICSVService;
import eye.on.the.money.service.stock.AccountService;
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
@RequiredArgsConstructor
@Slf4j
public class ETFInvestmentService implements ICSVService {
    private final ETFInvestmentRepository etfInvestmentRepository;
    private final ETFService etfService;
    private final CurrencyRepository currencyRepository;
    private final EODAPIService eodAPIService;
    private final UserService userService;
    private final ModelMapper modelMapper;
    private final AccountService accountService;
    public List<ETFInvestmentDTO> getETFInvestments(Long userId) {
        return this.etfInvestmentRepository.findByUserIdOrderByTransactionDateDesc(userId).stream().map(this::convertToETFInvestmentDTO).collect(Collectors.toList());
    }

    public List<ETFInvestmentDTO> getETFInvestmentsByAccountId(Long userId, Long accountId) {
        return this.etfInvestmentRepository.findByUserIdAndAccountIdOrderByTransactionDateDesc(userId, accountId)
                .stream().map(this::convertToETFInvestmentDTO).collect(Collectors.toList());
    }

    public List<ETFInvestmentDTO> getETFInvestmentsBetween(Long userId, LocalDate from, LocalDate to) {
        return this.etfInvestmentRepository.findByUserIdAndTransactionDateBetweenOrderByTransactionDate(userId, from, to)
                .stream().map(this::convertToETFInvestmentDTO).collect(Collectors.toList());
    }

    private ETFInvestmentDTO convertToETFInvestmentDTO(ETFInvestment etfInvestment) {
        return this.modelMapper.map(etfInvestment, ETFInvestmentDTO.class);
    }

    @Cacheable(cacheNames = "holdings-etf", key = "#userId")
    public List<ETFInvestmentDTO> getCurrentETFHoldings(Long userId) {
        return this.currentETFHoldings(userId);
    }

    @CachePut(cacheNames = "holdings-etf", key = "#userId")
    public List<ETFInvestmentDTO> refreshCurrentETFHoldings(Long userId) {
        return this.currentETFHoldings(userId);
    }

    private List<ETFInvestmentDTO> currentETFHoldings(Long userId) {
        List<ETFInvestmentDTO> investments = this.etfInvestmentRepository.findByUserIdOrderByTransactionDate(userId)
                .stream().map(this::convertToETFInvestmentDTO).toList();
        return this.getLiveDataForInvestments(investments);
    }

    public List<ETFInvestmentDTO> getHoldingsByAccountId(Long userId, Long accountId) {
        List<ETFInvestmentDTO> investments = this.etfInvestmentRepository.findByUserIdAndAccountIdOrderByTransactionDateDesc(userId, accountId)
                .stream().map(this::convertToETFInvestmentDTO).toList();
        return this.getLiveDataForInvestments(investments);
    }

    private List<ETFInvestmentDTO> getLiveDataForInvestments(List<ETFInvestmentDTO> investments) {
        Map<String, ETFInvestmentDTO> investmentMap = this.getCalculated(investments);
        List<ETFInvestmentDTO> etfInvestmentDTOList = (new ArrayList<>(investmentMap.values()))
                .stream().filter(i -> (i.getQuantity() > 0)).collect(Collectors.toList());
        if (etfInvestmentDTOList.isEmpty()) return etfInvestmentDTOList;

        String joinedList = etfInvestmentDTOList.stream().map(i -> Ticker.symbol(i.getShortName(), i.getExchange())).distinct().collect(Collectors.joining(","));

        try {
            JsonNode responseBody = this.eodAPIService.getLiveEtfValue(joinedList);
            for (JsonNode etf : responseBody) {
                String code = etf.findValue("code").textValue();
                Optional<LiveQuote.Price> price = LiveQuote.price(etf);
                if (price.isEmpty()) {
                    log.warn("No live or previous close for {}, leaving holding without live data", code);
                    continue;
                }
                etfInvestmentDTOList.stream()
                        .filter(i -> Ticker.symbol(i.getShortName(), i.getExchange()).equals(code))
                        .forEach(i -> {
                            i.setLiveValue(price.get().value() * i.getQuantity());
                            i.setValueDiff(i.getLiveValue() - i.getAmount());
                            i.setStalePrice(price.get().stale());
                            i.setDayChange(price.get().change() == null ? null : price.get().change() * i.getQuantity());
                            i.setDayChangePercent(price.get().changePercent());
                        });
            }
        } catch (APIException e) {
            log.error("Unable to fetch live ETF values, returning holdings without live data", e);
        }
        return etfInvestmentDTOList;
    }

    public List<ETFInvestmentDTO> getAllPositions(Long userId) {
        List<ETFInvestmentDTO> investments = this.etfInvestmentRepository.findByUserIdOrderByTransactionDate(userId)
                .stream().map(this::convertToETFInvestmentDTO).toList();
        Map<String, ETFInvestmentDTO> investmentMap = this.getCalculated(investments);
        return new ArrayList<>(investmentMap.values());
    }

    public List<ETFInvestmentDTO> getPositionsByAccountId(Long userId, Long accountId) {
        List<ETFInvestmentDTO> investments = this.etfInvestmentRepository.findByUserIdAndAccountIdOrderByTransactionDateDesc(userId, accountId)
                .stream().map(this::convertToETFInvestmentDTO).toList();
        Map<String, ETFInvestmentDTO> investmentMap = this.getCalculated(investments);
        return new ArrayList<>(investmentMap.values());
    }

    private Map<String, ETFInvestmentDTO> getCalculated(List<ETFInvestmentDTO> investments) {
        return Lots.aggregate(investments, i -> Ticker.symbol(i.getShortName(), i.getExchange()) + "_" + i.getAccountId());
    }


    @CacheEvict(cacheNames = "holdings-etf", key = "#userId")
    @Transactional
    public void deleteInvestmentById(Long userId, List<Long> ids) {
        this.etfInvestmentRepository.deleteByUserIdAndIdIn(userId, ids);
    }

    @CacheEvict(cacheNames = "holdings-etf", key = "#userId")
    @Transactional
    public ETFInvestmentDTO createInvestment(ETFInvestmentDTO investmentDTO, Long userId) {
        Currency currency = this.currencyRepository.findById(investmentDTO.getCurrencyId()).orElseThrow(() -> new NoSuchElementException("Currency not found: " + investmentDTO.getCurrencyId()));
        ETF etf = this.etfService.getOrCreateETF(investmentDTO.getShortName(), investmentDTO.getExchange(), investmentDTO.getName());
        User user = this.userService.getReference(userId);
        Account account = this.accountService.getAccount(userId, investmentDTO.getAccountId());

        ETFInvestment investment = ETFInvestment.builder()
                .buySell(investmentDTO.getBuySell())
                .creationDate(LocalDate.now())
                .transactionDate(investmentDTO.getTransactionDate())
                .user(user)
                .quantity(investmentDTO.getQuantity())
                .etf(etf)
                .amount(investmentDTO.getAmount())
                .currency(currency)
                .account(account)
                .fee(investmentDTO.getFee())
                .build();
        investment = this.etfInvestmentRepository.save(investment);
        return this.convertToETFInvestmentDTO(investment);
    }

    @CacheEvict(cacheNames = "holdings-etf", key = "#userId")
    @Transactional
    public ETFInvestmentDTO updateInvestment(ETFInvestmentDTO investmentDTO, Long userId) {
        Currency currency = this.currencyRepository.findById(investmentDTO.getCurrencyId()).orElseThrow(() -> new NoSuchElementException("Currency not found: " + investmentDTO.getCurrencyId()));
        ETF etf = this.etfService.getOrCreateETF(investmentDTO.getShortName(), investmentDTO.getExchange(), investmentDTO.getName());
        ETFInvestment investment = this.etfInvestmentRepository.findByIdAndUserId(investmentDTO.getId(), userId).orElseThrow(() -> new NoSuchElementException("ETF investment not found: " + investmentDTO.getId()));
        Account account = this.accountService.getAccount(userId, investmentDTO.getAccountId());

        investment.setBuySell(investmentDTO.getBuySell());
        investment.setTransactionDate(investmentDTO.getTransactionDate());
        investment.setQuantity(investmentDTO.getQuantity());
        investment.setEtf(etf);
        investment.setFee(investmentDTO.getFee());
        investment.setAmount(investmentDTO.getAmount());
        investment.setCurrency(currency);
        investment.setAccount(account);

        return this.convertToETFInvestmentDTO(investment);
    }

    public void getCSV(Long userId, Writer writer) {
        List<ETFInvestmentDTO> investmentList =
                this.etfInvestmentRepository.findByUserIdOrderByTransactionDate(userId)
                        .stream()
                        .map(this::convertToETFInvestmentDTO).
                        toList();
        this.printRecords(investmentList, writer);
    }

    @CacheEvict(cacheNames = "holdings-etf", key = "#userId")
    @Transactional
    public void processCSV(Long userId, MultipartFile file) {
        long lineNumber = 0;
        try (CSVParser csvParser = this.getParser(file,
                new String[]{"Investment Id", "Quantity", "Type", "Transaction Date", "Short Name", "Exchange", "Amount", "Currency", "Fee", "Account"})) {
            Map<String, Long> accountIdsByName = this.accountService.getAccountIdsByName(userId);

            for (CSVRecord csvRecord : csvParser) {
                lineNumber = csvParser.getCurrentLineNumber();
                ETFInvestmentDTO investment = ETFInvestmentDTO.createFromCSVRecord(csvRecord, DateFormats.YYYY_MM_DD);
                investment.setAccountId(this.resolveAccountId(accountIdsByName, investment.getAccountName()));

                if (investment.getId() != null &&
                        this.etfInvestmentRepository.findByIdAndUserId(investment.getId(), userId).isPresent()) {
                    log.trace("Update etf investment {}", investment);
                    this.updateInvestment(investment, userId);
                } else {
                    investment.setId(null);
                    log.trace("Create etf investment {}", investment);
                    this.createInvestment(investment, userId);
                }
            }
        } catch (IOException | DateTimeParseException | IllegalArgumentException e) {
            log.error("Error while processing CSV", e);
            throw this.csvParseFailure(lineNumber, e);
        }
    }
}
