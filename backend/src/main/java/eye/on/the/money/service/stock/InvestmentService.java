package eye.on.the.money.service.stock;

import eye.on.the.money.dto.out.InvestmentDTO;
import eye.on.the.money.model.User;
import org.springframework.web.multipart.MultipartFile;

import java.io.Writer;
import java.util.Date;
import java.util.List;

public interface InvestmentService {
    public List<InvestmentDTO> getInvestments(Long userId);
    public void deleteInvestmentById(User user, List<Long> ids);
    public List<InvestmentDTO> getCurrentHoldings(Long userId);
    public List<InvestmentDTO> getAllPositions(Long userId);
    public InvestmentDTO createInvestment(InvestmentDTO investmentDTO, User user);
    public void getCSV(Long userId, Writer writer);
    public InvestmentDTO updateInvestment(InvestmentDTO investmentDTO, User user);
    public void processCSV(User user, MultipartFile file);
    public List<InvestmentDTO> getInvestmentsByTypeAndDate(Long userId, String buySell, Date from, Date to);
}
