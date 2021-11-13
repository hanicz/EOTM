package eye.on.the.money.service;

import eye.on.the.money.dto.InvestmentDTO;

import java.util.List;

public interface InvestmentService {
    public List<InvestmentDTO> getInvestmentsByUserId(Long userId);
    public List<InvestmentDTO> getInvestmentsByUserIdWConvCurr(Long userId, String currency);
    public void deleteInvestmentById(Long id);
}
