package eye.on.the.money.service;

import eye.on.the.money.dto.in.InvestmentQuery;
import eye.on.the.money.dto.out.InvestmentDTO;
import eye.on.the.money.model.User;

import java.util.List;

public interface InvestmentService {
    public List<InvestmentDTO> getInvestments(Long userId);
    public List<InvestmentDTO> getInvestmentsByUserIdWConvCurr(Long userId, String currency);
    public List<InvestmentDTO> getInvestmentsByBuySell(Long userId, InvestmentQuery query);
    public List<InvestmentDTO> getInvestmentsByDate(Long userId, InvestmentQuery query);
    public List<InvestmentDTO> getInvestmentsByTypeAndDate(Long userId, InvestmentQuery query);
    public void deleteInvestmentById(Long id);
    public List<InvestmentDTO> getCurrentHoldings(Long userId, InvestmentQuery query);
    public List<InvestmentDTO> getAllPositions(Long userId, InvestmentQuery query);
    public InvestmentDTO createInvestment(InvestmentDTO investmentDTO, User user);
}
