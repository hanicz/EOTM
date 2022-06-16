package eye.on.the.money.service;

import eye.on.the.money.dto.out.ETFDividendDTO;
import eye.on.the.money.model.User;
import org.springframework.web.multipart.MultipartFile;

import java.io.Writer;
import java.util.List;

public interface ETFDividendService {
    public List<ETFDividendDTO> getDividends(Long userId);
    public ETFDividendDTO createETFDividend(ETFDividendDTO dividendDTO, User user);
    public ETFDividendDTO updateETFDividend(ETFDividendDTO dividendDTO, User user);
    public void deleteETFDividendById(List<Long> ids, User user);
    public void getCSV(Long userId, Writer writer);
    public void processCSV(User user, MultipartFile file);
}
