package eye.on.the.money.service.stock.impl;

import com.fasterxml.jackson.databind.JsonNode;
import eye.on.the.money.dto.in.InvestmentQuery;
import eye.on.the.money.dto.out.InvestmentDTO;
import eye.on.the.money.model.Currency;
import eye.on.the.money.model.User;
import eye.on.the.money.model.stock.Investment;
import eye.on.the.money.model.stock.Stock;
import eye.on.the.money.model.stock.StockPayment;
import eye.on.the.money.repository.forex.CurrencyRepository;
import eye.on.the.money.repository.stock.InvestmentRepository;
import eye.on.the.money.repository.stock.StockRepository;
import eye.on.the.money.service.api.EODAPIService;
import eye.on.the.money.service.stock.InvestmentService;
import eye.on.the.money.service.stock.StockPaymentService;
import eye.on.the.money.service.stock.StockService;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class InvestmentServiceImpl implements InvestmentService {

    @Autowired
    private InvestmentRepository investmentRepository;
    @Autowired
    private StockRepository stockRepository;
    @Autowired
    private CurrencyRepository currencyRepository;
    @Autowired
    private StockPaymentService stockPaymentService;
    @Autowired
    private ModelMapper modelMapper;
    @Autowired
    private EODAPIService eodAPIService;
    @Autowired
    private StockService stockService;

    @Override
    public List<InvestmentDTO> getInvestments(Long userId) {
        return this.investmentRepository.findByUser_IdOrderByTransactionDate(userId).stream().map(this::convertToInvestmentDTO).collect(Collectors.toList());
    }

    @Override
    public List<InvestmentDTO> getInvestmentsByTypeAndDate(Long userId, String buySell, Date from, Date to) {
        return this.investmentRepository.findByUser_IdAndBuySellAndTransactionDateBetween(userId, buySell, from, to)
                .stream().map(this::convertToInvestmentDTO).collect(Collectors.toList());
    }

    @Override
    public List<InvestmentDTO> getCurrentHoldings(Long userId, InvestmentQuery query) {
        Map<String, InvestmentDTO> investmentMap = this.getCalculated(userId, query);
        List<InvestmentDTO> investmentDTOList = (new ArrayList<InvestmentDTO>(investmentMap.values()))
                .stream().filter(i -> (i.getQuantity() > 0)).collect(Collectors.toList());
        String joinedList = investmentDTOList.stream().map(i -> (i.getShortName() + "." + i.getExchange())).collect(Collectors.joining(","));

        JsonNode responseBody = this.eodAPIService.getLiveValue(joinedList, "/real-time/stock/?api_token={0}&fmt=json&s={1}");

        for (JsonNode stock : responseBody) {
            Optional<InvestmentDTO> investmentDTO = investmentDTOList.stream().filter
                    (i -> (i.getShortName() + "." + i.getExchange()).equals(stock.findValue("code").textValue())).findFirst();
            if (investmentDTO.isEmpty()) continue;
            investmentDTO.get().setLiveValue(stock.findValue("close").doubleValue() * investmentDTO.get().getQuantity());
            investmentDTO.get().setValueDiff(investmentDTO.get().getLiveValue() - investmentDTO.get().getAmount());
        }

        return investmentDTOList;
    }

    @Override
    public List<InvestmentDTO> getAllPositions(Long userId, InvestmentQuery query) {
        Map<String, InvestmentDTO> investmentMap = this.getCalculated(userId, query);
        return new ArrayList<>((new ArrayList<>(investmentMap.values())));
    }

    private Map<String, InvestmentDTO> getCalculated(Long userId, InvestmentQuery query) {
        List<InvestmentDTO> investments = this.investmentRepository.findByUser_IdOrderByTransactionDate(userId).stream().map(this::convertToInvestmentDTO).collect(Collectors.toList());
        Map<String, InvestmentDTO> investmentMap = new HashMap<>();
        for (InvestmentDTO i : investments) {
            if (i.getBuySell().equals("S")) {
                i.negateAmountAndQuantity();
            }
            investmentMap.compute(i.getShortName(), (key, value) -> (value == null) ? i : value.mergeInvestments(i));
        }
        return investmentMap;
    }

    private InvestmentDTO convertToInvestmentDTO(Investment investment) {
        this.modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.LOOSE);
        return this.modelMapper.map(investment, InvestmentDTO.class);
    }

    @Transactional
    @Override
    public InvestmentDTO createInvestment(InvestmentDTO investmentDTO, User user) {
        Currency currency = this.currencyRepository.findById(investmentDTO.getCurrencyId()).orElseThrow(NoSuchElementException::new);
        Stock stock = this.stockService.getOrCreateStock(investmentDTO.getShortName(), investmentDTO.getExchange(), investmentDTO.getName());
        StockPayment stockPayment = this.stockPaymentService.createNewPayment(currency, investmentDTO.getAmount());

        Investment investment = Investment.builder()
                .buySell(investmentDTO.getBuySell())
                .creationDate(new Date())
                .transactionDate(investmentDTO.getTransactionDate())
                .user(user)
                .quantity(investmentDTO.getQuantity())
                .stock(stock)
                .stockPayment(stockPayment)
                .fee(investmentDTO.getFee())
                .build();
        investment = this.investmentRepository.save(investment);
        return this.convertToInvestmentDTO(investment);
    }

    @Transactional
    @Override
    public InvestmentDTO updateInvestment(InvestmentDTO investmentDTO, User user) {
        Currency currency = this.currencyRepository.findById(investmentDTO.getCurrencyId()).orElseThrow(NoSuchElementException::new);
        Stock stock = this.stockService.getOrCreateStock(investmentDTO.getShortName(), investmentDTO.getExchange(), investmentDTO.getName());
        Investment investment = this.investmentRepository.findByIdAndUser_Id(investmentDTO.getInvestmentId(), user.getId()).orElseThrow(NoSuchElementException::new);
        StockPayment stockPayment = investment.getStockPayment();

        investment.setBuySell(investmentDTO.getBuySell());
        investment.setTransactionDate(investmentDTO.getTransactionDate());
        investment.setQuantity(investmentDTO.getQuantity());
        investment.setStock(stock);
        investment.setFee(investmentDTO.getFee());
        stockPayment.setAmount(investmentDTO.getAmount());
        stockPayment.setCurrency(currency);

        return this.convertToInvestmentDTO(investment);
    }

    @Transactional
    @Override
    public void deleteInvestmentById(User user, List<Long> ids) {
        this.investmentRepository.deleteByUser_IdAndIdIn(user.getId(), ids);
    }

    @Override
    public void getCSV(Long userId, Writer writer) {
        List<InvestmentDTO> investmentList =
                this.investmentRepository.findByUser_IdOrderByTransactionDate(userId)
                        .stream()
                        .map(this::convertToInvestmentDTO).
                        collect(Collectors.toList());
        try (CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT)) {
            if (!investmentList.isEmpty()) {
                csvPrinter.printRecord("Investment Id", "Quantity", "Type", "Transaction Date", "Short Name", "Amount", "Currency", "Fee");
            }
            for (InvestmentDTO i : investmentList) {
                csvPrinter.printRecord(i.getInvestmentId(), i.getQuantity(),
                        i.getBuySell(), i.getTransactionDate(), i.getShortName(),
                        i.getAmount(), i.getCurrencyId(), i.getFee());
            }
        } catch (IOException e) {
            throw new RuntimeException("fail to create CSV file: " + e.getMessage());
        }
    }

    @Transactional
    @Override
    public void processCSV(User user, MultipartFile file) {
        try (BufferedReader fileReader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
             CSVParser csvParser = new CSVParser(fileReader, CSVFormat.Builder.create()
                     .setHeader("Investment Id", "Quantity", "Type", "Transaction Date", "Short Name", "Amount", "Currency", "Fee")
                     .setSkipHeaderRecord(true)
                     .setDelimiter(",")
                     .setTrim(true)
                     .setIgnoreHeaderCase(true)
                     .build())) {

            for (CSVRecord csvRecord : csvParser) {
                String investmentId = csvRecord.get("Investment Id");
                Date transactionDate = new SimpleDateFormat("yyyy-MM-dd").parse(csvRecord.get("Transaction Date"));

                InvestmentDTO investment = InvestmentDTO.builder()
                        .buySell(csvRecord.get("Type"))
                        .transactionDate(transactionDate)
                        .amount(Double.parseDouble(csvRecord.get("Amount")))
                        .quantity(Integer.parseInt(csvRecord.get("Quantity")))
                        .currencyId(csvRecord.get("Currency"))
                        .shortName(csvRecord.get("Short Name"))
                        .fee(Double.parseDouble(csvRecord.get("Fee")))
                        .build();
                log.trace(investment.toString());

                if (!investmentId.isEmpty() &&
                        this.investmentRepository.findByIdAndUser_Id(Long.parseLong(investmentId), user.getId()).isPresent()) {
                    investment.setInvestmentId(Long.parseLong(investmentId));
                    log.trace("Update investment " + investment.toString());
                    this.updateInvestment(investment, user);
                } else {
                    log.trace("Create investment " + investment.toString());
                    this.createInvestment(investment, user);
                }
            }
        } catch (IOException | ParseException e) {
            throw new RuntimeException("fail to parse CSV file: " + e.getMessage());
        }
    }
}
