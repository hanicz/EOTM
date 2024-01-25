package eye.on.the.money.service.etf;

import com.fasterxml.jackson.databind.JsonNode;
import eye.on.the.money.dto.out.ETFInvestmentDTO;
import eye.on.the.money.model.Currency;
import eye.on.the.money.model.User;
import eye.on.the.money.model.etf.ETF;
import eye.on.the.money.model.etf.ETFInvestment;
import eye.on.the.money.model.etf.ETFPayment;
import eye.on.the.money.repository.etf.ETFInvestmentRepository;
import eye.on.the.money.repository.etf.ETFRepository;
import eye.on.the.money.repository.forex.CurrencyRepository;
import eye.on.the.money.service.UserServiceImpl;
import eye.on.the.money.service.api.EODAPIService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.Writer;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ETFInvestmentService {
    private final ETFInvestmentRepository etfInvestmentRepository;
    private final ETFRepository etfRepository;
    private final CurrencyRepository currencyRepository;
    private final EODAPIService eodAPIService;
    private final UserServiceImpl userService;
    private final ModelMapper modelMapper;
    private final ETFPaymentService etfPaymentService;

    public List<ETFInvestmentDTO> getETFInvestments(String userEmail) {
        return this.etfInvestmentRepository.findByUserEmailOrderByTransactionDate(userEmail).stream().map(this::convertToETFInvestmentDTO).collect(Collectors.toList());
    }

    private ETFInvestmentDTO convertToETFInvestmentDTO(ETFInvestment etfInvestment) {
        this.modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.LOOSE);
        return this.modelMapper.map(etfInvestment, ETFInvestmentDTO.class);
    }

    public List<ETFInvestmentDTO> getCurrentETFHoldings(String userEmail) {
        Map<String, ETFInvestmentDTO> investmentMap = this.getCalculated(userEmail);
        List<ETFInvestmentDTO> etfInvestmentDTOList = (new ArrayList<>(investmentMap.values()))
                .stream().filter(i -> (i.getQuantity() > 0)).collect(Collectors.toList());
        String joinedList = etfInvestmentDTOList.stream().map(i -> (i.getShortName() + "." + i.getExchange())).collect(Collectors.joining(","));

        JsonNode responseBody = this.eodAPIService.getLiveValue(joinedList, "/real-time/etf/?api_token={0}&fmt=json&s={1}");

        for (JsonNode etf : responseBody) {
            Optional<ETFInvestmentDTO> etfInvestmentDTO = etfInvestmentDTOList.stream().filter
                    (i -> (i.getShortName() + "." + i.getExchange()).equals(etf.findValue("code").textValue())).findFirst();
            if (etfInvestmentDTO.isEmpty()) continue;
            etfInvestmentDTO.get().setLiveValue(etf.findValue("close").doubleValue() * etfInvestmentDTO.get().getQuantity());
            etfInvestmentDTO.get().setValueDiff(etfInvestmentDTO.get().getLiveValue() - etfInvestmentDTO.get().getAmount());
        }
        return etfInvestmentDTOList;
    }

    public List<ETFInvestmentDTO> getAllPositions(String userEmail) {
        Map<String, ETFInvestmentDTO> investmentMap = this.getCalculated(userEmail);
        return (new ArrayList<>(investmentMap.values()));
    }

    private Map<String, ETFInvestmentDTO> getCalculated(String userEmail) {
        List<ETFInvestmentDTO> investments = this.etfInvestmentRepository.findByUserEmailOrderByTransactionDate(userEmail).stream().map(this::convertToETFInvestmentDTO).toList();

        Map<String, ETFInvestmentDTO> investmentMap = new HashMap<>();
        for (ETFInvestmentDTO i : investments) {
            if (i.getBuySell().equals("S")) {
                i.negateAmountAndQuantity();
            }
            investmentMap.compute(i.getShortName(), (key, value) -> (value == null) ? i : value.mergeInvestments(i));
        }
        return investmentMap;
    }


    @Transactional
    public void deleteInvestmentById(String userEmail, List<Long> ids) {
        this.etfInvestmentRepository.deleteByUserEmailAndIdIn(userEmail, ids);
    }

    @Transactional
    public ETFInvestmentDTO createInvestment(ETFInvestmentDTO investmentDTO, String userEmail) {
        Currency currency = this.currencyRepository.findById(investmentDTO.getCurrencyId()).orElseThrow(NoSuchElementException::new);
        ETF etf = this.etfRepository.findByShortNameAndExchange(investmentDTO.getShortName(), investmentDTO.getExchange()).orElseThrow(NoSuchElementException::new);
        ETFPayment etfPayment = this.etfPaymentService.createPayment(currency, investmentDTO.getAmount());
        User user = this.userService.loadUserByEmail(userEmail);

        ETFInvestment investment = ETFInvestment.builder()
                .buySell(investmentDTO.getBuySell())
                .creationDate(new Date())
                .transactionDate(investmentDTO.getTransactionDate())
                .user(user)
                .quantity(investmentDTO.getQuantity())
                .etf(etf)
                .etfPayment(etfPayment)
                .fee(investmentDTO.getFee())
                .build();
        investment = this.etfInvestmentRepository.save(investment);
        return this.convertToETFInvestmentDTO(investment);
    }

    @Transactional
    public ETFInvestmentDTO updateInvestment(ETFInvestmentDTO investmentDTO, String userEmail) {
        Currency currency = this.currencyRepository.findById(investmentDTO.getCurrencyId()).orElseThrow(NoSuchElementException::new);
        ETF etf = this.etfRepository.findByShortNameAndExchange(investmentDTO.getShortName(), investmentDTO.getExchange()).orElseThrow(NoSuchElementException::new);
        ETFInvestment investment = this.etfInvestmentRepository.findByIdAndUserEmail(investmentDTO.getId(), userEmail).orElseThrow(NoSuchElementException::new);
        ETFPayment etfPayment = investment.getEtfPayment();

        investment.setBuySell(investmentDTO.getBuySell());
        investment.setTransactionDate(investmentDTO.getTransactionDate());
        investment.setQuantity(investmentDTO.getQuantity());
        investment.setEtf(etf);
        investment.setFee(investmentDTO.getFee());
        etfPayment.setAmount(investmentDTO.getAmount());
        etfPayment.setCurrency(currency);

        return this.convertToETFInvestmentDTO(investment);
    }

    public void getCSV(String userEmail, Writer writer) {
        List<ETFInvestmentDTO> investmentList =
                this.etfInvestmentRepository.findByUserEmailOrderByTransactionDate(userEmail)
                        .stream()
                        .map(this::convertToETFInvestmentDTO).
                        toList();
        try (CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT)) {
            if (!investmentList.isEmpty()) {
                csvPrinter.printRecord("Investment Id", "Quantity", "Type", "Transaction Date", "Short Name", "Exchange", "Amount", "Currency", "Fee");
            }
            for (ETFInvestmentDTO i : investmentList) {
                csvPrinter.printRecord(i.getId(), i.getQuantity(),
                        i.getBuySell(), i.getTransactionDate(), i.getShortName(), i.getExchange(),
                        i.getAmount(), i.getCurrencyId(), i.getFee());
            }
        } catch (IOException e) {
            throw new RuntimeException("fail to create CSV file: " + e.getMessage());
        }
    }
}
