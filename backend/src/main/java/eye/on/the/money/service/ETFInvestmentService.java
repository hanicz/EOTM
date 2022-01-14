package eye.on.the.money.service;

import eye.on.the.money.dto.in.InvestmentQuery;
import eye.on.the.money.dto.out.ETFInvestmentDTO;

import java.util.List;

public interface ETFInvestmentService {
    public List<ETFInvestmentDTO> getETFInvestments(Long userId);
    public List<ETFInvestmentDTO> getCurrentETFHoldings(Long userId, InvestmentQuery query);
    public void updateETFPrices();
}
