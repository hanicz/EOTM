package eye.on.the.money.service.impl;

import eye.on.the.money.dto.in.InvestmentQuery;
import eye.on.the.money.dto.out.ETFInvestmentDTO;
import eye.on.the.money.model.etf.ETF;
import eye.on.the.money.model.etf.ETFInvestment;
import eye.on.the.money.repository.etf.ETFInvestmentRepository;
import eye.on.the.money.repository.etf.ETFRepository;
import eye.on.the.money.service.ETFInvestmentService;
import eye.on.the.money.service.api.CurrencyConverter;
import eye.on.the.money.service.api.ETFAPIService;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ETFInvestmentServiceImpl implements ETFInvestmentService {

    @Autowired
    private ETFInvestmentRepository etfInvestmentRepository;

    @Autowired
    private ETFRepository etfRepository;

    @Autowired
    private ETFAPIService etfapiService;

    @Autowired
    private CurrencyConverter currencyConverter;

    @Autowired
    private ModelMapper modelMapper;

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
}
