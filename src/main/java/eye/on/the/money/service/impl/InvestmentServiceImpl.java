package eye.on.the.money.service.impl;

import eye.on.the.money.dto.in.InvestmentQuery;
import eye.on.the.money.dto.out.InvestmentDTO;
import eye.on.the.money.model.Currency;
import eye.on.the.money.model.User;
import eye.on.the.money.model.stock.Investment;
import eye.on.the.money.model.stock.Stock;
import eye.on.the.money.model.stock.StockPayment;
import eye.on.the.money.repository.CurrencyRepository;
import eye.on.the.money.repository.InvestmentRepository;
import eye.on.the.money.repository.StockRepository;
import eye.on.the.money.service.InvestmentService;
import eye.on.the.money.service.StockPaymentService;
import eye.on.the.money.service.currency.CurrencyConverter;
import eye.on.the.money.service.currency.StockAPIService;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class InvestmentServiceImpl implements InvestmentService {

    @Autowired
    private InvestmentRepository investmentRepository;

    @Autowired
    private CurrencyConverter currencyConverter;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private CurrencyRepository currencyRepository;

    @Autowired
    private StockPaymentService stockPaymentService;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private StockAPIService stockAPIService;

    @Override
    public List<InvestmentDTO> getInvestments(Long userId) {
        return this.investmentRepository.findByUser_Id(userId).stream().map(this::convertToInvestmentDTO).collect(Collectors.toList());
    }

    @Override
    public List<InvestmentDTO> getInvestmentsByUserIdWConvCurr(Long userId, String currency) {
        List<InvestmentDTO> investments = this.investmentRepository.findByUser_Id(userId).stream().map(this::convertToInvestmentDTO).collect(Collectors.toList());
        this.currencyConverter.changeInvestmentsCurrency(investments, currency);
        return investments;
    }

    @Override
    public List<InvestmentDTO> getInvestmentsByBuySell(Long userId, InvestmentQuery query) {
        List<InvestmentDTO> investments = this.investmentRepository.findByUser_IdAndBuySell(userId, query.getType())
                .stream().map(this::convertToInvestmentDTO).collect(Collectors.toList());
        this.currencyConverter.changeInvestmentsCurrency(investments, query.getCurrency());
        return investments;
    }

    @Override
    public List<InvestmentDTO> getInvestmentsByDate(Long userId, InvestmentQuery query) {
        List<InvestmentDTO> investments = this.investmentRepository.findByUser_IdAndTransactionDateBetween(userId, query.getTransactionDateStart(), query.getTransactionDateEnd())
                .stream().map(this::convertToInvestmentDTO).collect(Collectors.toList());
        this.currencyConverter.changeInvestmentsCurrency(investments, query.getCurrency());
        return investments;
    }

    @Override
    public List<InvestmentDTO> getInvestmentsByTypeAndDate(Long userId, InvestmentQuery query) {
        List<InvestmentDTO> investments =
                this.investmentRepository.findByUser_IdAndBuySellAndTransactionDateBetween(userId, query.getType(), query.getTransactionDateStart(), query.getTransactionDateEnd())
                        .stream().map(this::convertToInvestmentDTO).collect(Collectors.toList());
        this.currencyConverter.changeInvestmentsCurrency(investments, query.getCurrency());
        return investments;
    }

    @Override
    public List<InvestmentDTO> getCurrentHoldings(Long userId, InvestmentQuery query) {
        Map<String, InvestmentDTO> investmentMap = this.getCalculated(userId, query);
        List<InvestmentDTO> investmentDTOList = (new ArrayList<InvestmentDTO>(investmentMap.values()))
                .stream().filter(i -> (i.getQuantity() > 0)).collect(Collectors.toList());
        this.stockAPIService.getLiveValue(investmentDTOList);
        this.currencyConverter.changeLiveValueCurrency(investmentDTOList, query.getCurrency());
        return investmentDTOList;

    }

    @Override
    public List<InvestmentDTO> getAllPositions(Long userId, InvestmentQuery query) {
        Map<String, InvestmentDTO> investmentMap = this.getCalculated(userId, query);
        return (new ArrayList<InvestmentDTO>(investmentMap.values())).stream().map(investment -> {
            investment.setCurrencyId(query.getCurrency());
            return investment;
        }).collect(Collectors.toList());

    }

    private Map<String, InvestmentDTO> getCalculated(Long userId, InvestmentQuery query) {
        List<InvestmentDTO> investments = this.investmentRepository.findByUser_Id(userId).stream().map(this::convertToInvestmentDTO).collect(Collectors.toList());
        this.currencyConverter.changeInvestmentsCurrency(investments, query.getCurrency());
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
        Stock stock = this.stockRepository.findByShortName(investmentDTO.getShortName()).orElseThrow(NoSuchElementException::new);
        StockPayment stockPayment = this.stockPaymentService.createNewPayment(currency, investmentDTO.getAmount());

        Investment investment = Investment.builder()
                .buySell(investmentDTO.getBuySell())
                .creationDate(new Date())
                .transactionDate(investmentDTO.getTransactionDate())
                .user(user)
                .quantity(investmentDTO.getQuantity())
                .stock(stock)
                .stockPayment(stockPayment)
                .build();
        investment = this.investmentRepository.save(investment);
        return this.convertToInvestmentDTO(investment);
    }

    @Override
    public void deleteInvestmentById(Long id) {
    }
}
