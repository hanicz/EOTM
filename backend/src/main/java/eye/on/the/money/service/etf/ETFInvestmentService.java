package eye.on.the.money.service.etf;

import eye.on.the.money.dto.out.ETFInvestmentDTO;
import eye.on.the.money.model.User;

import java.io.Writer;
import java.util.List;

public interface ETFInvestmentService {
    public List<ETFInvestmentDTO> getETFInvestments(Long userId);
    public List<ETFInvestmentDTO> getCurrentETFHoldings(Long userId);
    public void deleteInvestmentById(User user, List<Long> ids);
    public List<ETFInvestmentDTO> getAllPositions(Long userId);
    public ETFInvestmentDTO createInvestment(ETFInvestmentDTO investmentDTO, User user);
    public void getCSV(Long userId, Writer writer);
    public ETFInvestmentDTO updateInvestment(ETFInvestmentDTO investmentDTO, User user);
}
