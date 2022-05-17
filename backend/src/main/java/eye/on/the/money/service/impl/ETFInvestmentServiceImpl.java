package eye.on.the.money.service.impl;

import eye.on.the.money.dto.in.InvestmentQuery;
import eye.on.the.money.dto.out.ETFInvestmentDTO;
import eye.on.the.money.model.User;
import eye.on.the.money.model.etf.ETF;
import eye.on.the.money.model.etf.ETFInvestment;
import eye.on.the.money.model.etf.ETFPayment;
import eye.on.the.money.model.forex.Currency;
import eye.on.the.money.repository.etf.ETFInvestmentRepository;
import eye.on.the.money.repository.etf.ETFRepository;
import eye.on.the.money.repository.forex.CurrencyRepository;
import eye.on.the.money.service.ETFInvestmentService;
import eye.on.the.money.service.ETFPaymentService;
import eye.on.the.money.service.api.CurrencyConverter;
import eye.on.the.money.service.api.ETFAPIService;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.Writer;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ETFInvestmentServiceImpl implements ETFInvestmentService {

    @Autowired
    private ETFInvestmentRepository etfInvestmentRepository;

    @Autowired
    private ETFRepository etfRepository;

    @Autowired
    private CurrencyRepository currencyRepository;

    @Autowired
    private ETFAPIService etfapiService;

    @Autowired
    private CurrencyConverter currencyConverter;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private ETFPaymentService etfPaymentService;

    @Override
    public List<ETFInvestmentDTO> getETFInvestments(Long userId) {
        return this.etfInvestmentRepository.findByUser_IdOrderByTransactionDate(userId).stream().map(this::convertToETFInvestmentDTO).collect(Collectors.toList());
    }

    private ETFInvestmentDTO convertToETFInvestmentDTO(ETFInvestment etfInvestment) {
        this.modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.LOOSE);
        return this.modelMapper.map(etfInvestment, ETFInvestmentDTO.class);
    }

    @Override
    public List<ETFInvestmentDTO> getCurrentETFHoldings(Long userId, InvestmentQuery query) {
        Map<String, ETFInvestmentDTO> investmentMap = this.getCalculated(userId, query);
        return (new ArrayList<ETFInvestmentDTO>(investmentMap.values()))
                .stream().filter(i -> (i.getQuantity() > 0)).collect(Collectors.toList());
    }

    @Override
    public List<ETFInvestmentDTO> getAllPositions(Long userId, InvestmentQuery query) {
        Map<String, ETFInvestmentDTO> investmentMap = this.getCalculated(userId, query);
        return (new ArrayList<ETFInvestmentDTO>(investmentMap.values()));
    }

    private Map<String, ETFInvestmentDTO> getCalculated(Long userId, InvestmentQuery query) {
        List<ETFInvestmentDTO> investments = this.etfInvestmentRepository.findByUser_IdOrderByTransactionDate(userId).stream().map(this::convertToETFInvestmentDTO).collect(Collectors.toList());
        for(ETFInvestmentDTO i : investments){
            if(i.getCurrencyId().equals(query.getCurrency())){
                i.setLiveValue(i.getLiveValue() * i.getQuantity());
                i.setValueDiff(i.getLiveValue() - i.getAmount());
            }
        }
        this.currencyConverter.changeETFCurrency(investments, query.getCurrency());
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
    @Override
    public void updateETFPrices() {
        List<ETF> etfList = this.etfRepository.findAllByOrderByShortNameAsc();
        this.etfapiService.updateETFPrices(etfList);
    }

    @Transactional
    @Override
    public void deleteInvestmentById(User user, List<Long> ids) {
        this.etfInvestmentRepository.deleteByUser_IdAndIdIn(user.getId(), ids);
    }

    @Transactional
    @Override
    public ETFInvestmentDTO createInvestment(ETFInvestmentDTO investmentDTO, User user) {
        Currency currency = this.currencyRepository.findById(investmentDTO.getCurrencyId()).orElseThrow(NoSuchElementException::new);
        ETF etf = this.etfRepository.findByShortNameAndExchange(investmentDTO.getShortName(), investmentDTO.getExchange()).orElseThrow(NoSuchElementException::new);
        ETFPayment etfPayment = this.etfPaymentService.createPayment(currency, investmentDTO.getAmount());

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
    @Override
    public ETFInvestmentDTO updateInvestment(ETFInvestmentDTO investmentDTO, User user) {
        Currency currency = this.currencyRepository.findById(investmentDTO.getCurrencyId()).orElseThrow(NoSuchElementException::new);
        ETF etf = this.etfRepository.findByShortNameAndExchange(investmentDTO.getShortName(), investmentDTO.getExchange()).orElseThrow(NoSuchElementException::new);
        ETFInvestment investment = this.etfInvestmentRepository.findByIdAndUser_Id(investmentDTO.getId(), user.getId()).orElseThrow(NoSuchElementException::new);
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

    @Override
    public void getCSV(Long userId, Writer writer) {
        List<ETFInvestmentDTO> investmentList =
                this.etfInvestmentRepository.findByUser_IdOrderByTransactionDate(userId)
                        .stream()
                        .map(this::convertToETFInvestmentDTO).
                        collect(Collectors.toList());
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
