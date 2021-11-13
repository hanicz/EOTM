package eye.on.the.money.service.impl;

import eye.on.the.money.dto.InvestmentDTO;
import eye.on.the.money.model.stock.Investment;
import eye.on.the.money.repository.InvestmentRepository;
import eye.on.the.money.service.InvestmentService;
import eye.on.the.money.service.currency.CurrencyConverter;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
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
    public List<InvestmentDTO> getInvestmentsByUserId(Long userId) {
        return this.investmentRepository.findByUser_Id(userId).stream().map(this::convertToInvestmentDTO).collect(Collectors.toList());
    }

    public List<InvestmentDTO> getInvestmentsByUserIdWConvCurr(Long userId, String currency) {
        List<InvestmentDTO> investments = this.investmentRepository.findByUser_Id(userId).stream().map(this::convertToInvestmentDTO).collect(Collectors.toList());
        this.currencyConverter.changeInvestmentsToCurrency(investments, currency);
        return investments;
    }

    private InvestmentDTO convertToInvestmentDTO(Investment investment){
        this.modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.LOOSE);
        return this.modelMapper.map(investment, InvestmentDTO.class);
    }

    @Override
    public void deleteInvestmentById(Long id) {

    }
}
