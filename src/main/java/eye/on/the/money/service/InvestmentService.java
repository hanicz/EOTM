package eye.on.the.money.service;

import eye.on.the.money.dto.in.InvestmentIn;
import eye.on.the.money.dto.out.InvestmentDTO;

import java.util.List;

public interface InvestmentService {
    public List<InvestmentDTO> getInvestments(Long userId);
    public List<InvestmentDTO> getInvestmentsByUserIdWConvCurr(Long userId, String currency);
    public List<InvestmentDTO> getInvestmentsByBuySell(Long userId, InvestmentIn query);
    public List<InvestmentDTO> getInvestmentsByDate(Long userId, InvestmentIn query);
    public List<InvestmentDTO> getInvestmentsByTypeAndDate(Long userId, InvestmentIn query);
    public void deleteInvestmentById(Long id);
    public List<InvestmentDTO> getCurrentHoldings(Long userId, InvestmentIn query);
    public List<InvestmentDTO> getAllPositions(Long userId, InvestmentIn query);
}
