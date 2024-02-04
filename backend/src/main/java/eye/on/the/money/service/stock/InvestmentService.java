package eye.on.the.money.service.stock;

import com.fasterxml.jackson.databind.JsonNode;
import eye.on.the.money.dto.out.InvestmentDTO;
import eye.on.the.money.exception.CSVException;
import eye.on.the.money.model.Currency;
import eye.on.the.money.model.User;
import eye.on.the.money.model.stock.Investment;
import eye.on.the.money.model.stock.Stock;
import eye.on.the.money.model.stock.StockPayment;
import eye.on.the.money.repository.forex.CurrencyRepository;
import eye.on.the.money.repository.stock.InvestmentRepository;
import eye.on.the.money.service.CSVService;
import eye.on.the.money.service.UserServiceImpl;
import eye.on.the.money.service.api.EODAPIService;
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
@Slf4j
@RequiredArgsConstructor
public class InvestmentService {

    private final InvestmentRepository investmentRepository;
    private final CurrencyRepository currencyRepository;
    private final StockPaymentService stockPaymentService;
    private final UserServiceImpl userService;
    private final ModelMapper modelMapper;
    private final EODAPIService eodAPIService;
    private final StockService stockService;
    private final CSVService csvService;

    private final static DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public List<InvestmentDTO> getInvestments(String userEmail) {
        return this.investmentRepository.findByUserEmailOrderByTransactionDate(userEmail).stream().map(this::convertToInvestmentDTO).collect(Collectors.toList());
    }

    public List<InvestmentDTO> getCurrentHoldings(String userEmail) {
        Map<String, InvestmentDTO> investmentMap = this.getCalculated(userEmail);
        List<InvestmentDTO> investmentDTOList = (new ArrayList<>(investmentMap.values()))
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

    public List<InvestmentDTO> getAllPositions(String userEmail) {
        Map<String, InvestmentDTO> investmentMap = this.getCalculated(userEmail);
        return new ArrayList<>(investmentMap.values());
    }

    private Map<String, InvestmentDTO> getCalculated(String userEmail) {
        List<InvestmentDTO> investments = this.investmentRepository.findByUserEmailOrderByTransactionDate(userEmail).stream().map(this::convertToInvestmentDTO).toList();
        Map<String, InvestmentDTO> investmentMap = new HashMap<>();
        investments.forEach(i -> {
            if (i.getBuySell().equals("S")) {
                i.negateAmountAndQuantity();
            }
            investmentMap.compute(i.getShortName(), (key, value) -> (value == null) ? i : value.mergeInvestments(i));
        });
        return investmentMap;
    }

    private InvestmentDTO convertToInvestmentDTO(Investment investment) {
        return this.modelMapper.map(investment, InvestmentDTO.class);
    }

    @Transactional
    public InvestmentDTO createInvestment(InvestmentDTO investmentDTO, String userEmail) {
        Currency currency = this.currencyRepository.findById(investmentDTO.getCurrencyId()).orElseThrow(NoSuchElementException::new);
        Stock stock = this.stockService.getOrCreateStock(investmentDTO.getShortName(), investmentDTO.getExchange(), investmentDTO.getName());
        StockPayment stockPayment = this.stockPaymentService.createNewPayment(currency, investmentDTO.getAmount());
        User user = this.userService.loadUserByEmail(userEmail);

        Investment investment = Investment.builder()
                .buySell(investmentDTO.getBuySell())
                .creationDate(LocalDate.now())
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
    public InvestmentDTO updateInvestment(InvestmentDTO investmentDTO, String userEmail) {
        Currency currency = this.currencyRepository.findById(investmentDTO.getCurrencyId()).orElseThrow(NoSuchElementException::new);
        Stock stock = this.stockService.getOrCreateStock(investmentDTO.getShortName(), investmentDTO.getExchange(), investmentDTO.getName());
        Investment investment = this.investmentRepository.findByIdAndUserEmail(investmentDTO.getInvestmentId(), userEmail).orElseThrow(NoSuchElementException::new);
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
    public void deleteInvestmentById(String userEmail, String ids) {
        List<Long> idList = Stream.of(ids.split(",")).map(s -> Long.parseLong(s.trim())).collect(Collectors.toList());
        this.investmentRepository.deleteByUserEmailAndIdIn(userEmail, idList);
    }

    public void getCSV(String userEmail, Writer writer) {
        List<InvestmentDTO> investmentList =
                this.investmentRepository.findByUserEmailOrderByTransactionDate(userEmail)
                        .stream()
                        .map(this::convertToInvestmentDTO)
                        .toList();
        this.csvService.getCSV(investmentList, writer);
    }

    @Transactional
    public void processCSV(String userEmail, MultipartFile file) {
        try (CSVParser csvParser = this.csvService.getParser(file,
                new String[]{"Investment Id", "Quantity", "Type", "Transaction Date", "Short Name", "Amount", "Currency", "Fee"})) {
            for (CSVRecord csvRecord : csvParser) {
                InvestmentDTO investment = InvestmentDTO.createFromCSVRecord(csvRecord, FORMATTER);

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
        } catch (IOException | DateTimeParseException e) {
            log.error("Error while processing CSV", e);
            throw new CSVException("Failed to parse CSV file: " + e.getMessage(), e);
        }
    }
}
