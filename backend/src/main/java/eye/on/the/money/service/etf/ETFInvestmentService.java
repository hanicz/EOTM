package eye.on.the.money.service.etf;

import eye.on.the.money.dto.out.ETFInvestmentDTO;

import java.io.Writer;
import java.util.List;

public interface ETFInvestmentService {
    public List<ETFInvestmentDTO> getETFInvestments(String userEmail);

    public List<ETFInvestmentDTO> getCurrentETFHoldings(String userEmail);

    public void deleteInvestmentById(String userEmail, List<Long> ids);

    public List<ETFInvestmentDTO> getAllPositions(String userEmail);

    public ETFInvestmentDTO createInvestment(ETFInvestmentDTO investmentDTO, String userEmail);

    public void getCSV(String userEmail, Writer writer);

    public ETFInvestmentDTO updateInvestment(ETFInvestmentDTO investmentDTO, String userEmail);
}
