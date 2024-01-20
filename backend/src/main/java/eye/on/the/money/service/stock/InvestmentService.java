package eye.on.the.money.service.stock;

import eye.on.the.money.dto.out.InvestmentDTO;
import eye.on.the.money.model.User;
import org.springframework.web.multipart.MultipartFile;

import java.io.Writer;
import java.util.Date;
import java.util.List;

public interface InvestmentService {
    public List<InvestmentDTO> getInvestments(String userEmail);
    public void deleteInvestmentById(String userEmail, List<Long> ids);
    public List<InvestmentDTO> getCurrentHoldings(String userEmail);
    public List<InvestmentDTO> getAllPositions(String userEmail);
    public InvestmentDTO createInvestment(InvestmentDTO investmentDTO, String userEmail);
    public void getCSV(String userEmail, Writer writer);
    public InvestmentDTO updateInvestment(InvestmentDTO investmentDTO, String userEmail);
    public void processCSV(String userEmail, MultipartFile file);
    public List<InvestmentDTO> getInvestmentsByTypeAndDate(String userEmail, String buySell, Date from, Date to);
}
