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
import eye.on.the.money.service.UserServiceImpl;
import eye.on.the.money.service.api.EODAPIService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

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
    public void deleteInvestmentById(String userEmail, List<Long> ids) {
        this.investmentRepository.deleteByUserEmailAndIdIn(userEmail, ids);
    }

    public void getCSV(String userEmail, Writer writer) {
        List<InvestmentDTO> investmentList =
                this.investmentRepository.findByUserEmailOrderByTransactionDate(userEmail)
                        .stream()
                        .map(this::convertToInvestmentDTO)
                        .toList();
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
            throw new CSVException("Failed to create CSV file: " + e.getMessage(), e);
        }
    }

    @Transactional
    public void processCSV(String userEmail, MultipartFile file) {
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
                LocalDate transactionDate = LocalDate.parse(csvRecord.get("Transaction Date"), FORMATTER);

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
                        this.investmentRepository.findByIdAndUserEmail(Long.parseLong(investmentId), userEmail).isPresent()) {
                    investment.setInvestmentId(Long.parseLong(investmentId));
                    log.trace("Update investment {}", investment);
                    this.updateInvestment(investment, userEmail);
                } else {
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
