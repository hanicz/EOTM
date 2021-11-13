package eye.on.the.money.service.impl;

import eye.on.the.money.dto.in.InvestmentIn;
import eye.on.the.money.dto.out.InvestmentDTO;
import eye.on.the.money.model.stock.Investment;
import eye.on.the.money.repository.InvestmentRepository;
import eye.on.the.money.service.InvestmentService;
import eye.on.the.money.service.currency.CurrencyConverter;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class InvestmentServiceImpl implements InvestmentService {

    @Autowired
    private InvestmentRepository investmentRepository;

    @Autowired
    private CurrencyConverter currencyConverter;

    @Autowired
    private ModelMapper modelMapper;

    @Override
    public List<InvestmentDTO> getInvestments(Long userId) {
        return this.investmentRepository.findByUser_Id(userId).stream().map(this::convertToInvestmentDTO).collect(Collectors.toList());
    }

    @Override
    public List<InvestmentDTO> getInvestmentsByUserIdWConvCurr(Long userId, String currency) {
        List<InvestmentDTO> investments = this.investmentRepository.findByUser_Id(userId).stream().map(this::convertToInvestmentDTO).collect(Collectors.toList());
        this.currencyConverter.changeInvestmentsToCurrency(investments, currency);
        return investments;
    }

    @Override
    public List<InvestmentDTO> getInvestmentsByBuySell(Long userId, InvestmentIn query) {
        List<InvestmentDTO> investments = this.investmentRepository.findByUser_IdAndBuySell(userId, query.getType())
                .stream().map(this::convertToInvestmentDTO).collect(Collectors.toList());
        this.currencyConverter.changeInvestmentsToCurrency(investments, query.getCurrency());
        return investments;
    }

    @Override
    public List<InvestmentDTO> getInvestmentsByDate(Long userId, InvestmentIn query) {
        List<InvestmentDTO> investments = this.investmentRepository.findByUser_IdAndTransactionDateBetween(userId, query.getTransactionDateStart(), query.getTransactionDateEnd())
                .stream().map(this::convertToInvestmentDTO).collect(Collectors.toList());
        this.currencyConverter.changeInvestmentsToCurrency(investments, query.getCurrency());
        return investments;
    }

    @Override
    public List<InvestmentDTO> getInvestmentsByTypeAndDate(Long userId, InvestmentIn query) {
        List<InvestmentDTO> investments =
                this.investmentRepository.findByUser_IdAndBuySellAndTransactionDateBetween(userId, query.getType(), query.getTransactionDateStart(), query.getTransactionDateEnd())
                .stream().map(this::convertToInvestmentDTO).collect(Collectors.toList());
        this.currencyConverter.changeInvestmentsToCurrency(investments, query.getCurrency());
        return investments;
    }

    @Override
    public List<InvestmentDTO> getCurrentHoldings(Long userId, InvestmentIn query){
        Map<String, InvestmentDTO> investmentMap = this.getCalculated(userId, query);
        return (new ArrayList<InvestmentDTO>(investmentMap.values())).stream().filter(i -> (i.getQuantity() > 0)).collect(Collectors.toList());

    }

    @Override
    public List<InvestmentDTO> getAllPositions(Long userId, InvestmentIn query){
        Map<String, InvestmentDTO> investmentMap = this.getCalculated(userId, query);
        return (new ArrayList<InvestmentDTO>(investmentMap.values()));

    }

    private Map<String, InvestmentDTO> getCalculated(Long userId, InvestmentIn query){
        List<InvestmentDTO> investments = this.investmentRepository.findByUser_Id(userId).stream().map(this::convertToInvestmentDTO).collect(Collectors.toList());
        this.currencyConverter.changeInvestmentsToCurrency(investments, query.getCurrency());
        Map<String, InvestmentDTO> investmentMap = new HashMap<>();
        for(InvestmentDTO i : investments){
            if(i.getBuySell().equals("S")){
                i.negateAmountAndQuantity();
            }
            investmentMap.compute(i.getShortName(), (key, value) -> (value == null) ? i : value.mergeInvestments(i));
        }
        return investmentMap;
    }

    private InvestmentDTO convertToInvestmentDTO(Investment investment){
        this.modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.LOOSE);
        return this.modelMapper.map(investment, InvestmentDTO.class);
    }

    @Override
    public void deleteInvestmentById(Long id) {

    }
}
