package eye.on.the.money.service.etf;

import eye.on.the.money.dto.out.ETFDividendDTO;
import org.springframework.web.multipart.MultipartFile;

import java.io.Writer;
import java.util.List;

public interface ETFDividendService {
    public List<ETFDividendDTO> getDividends(String userEmail);

    public ETFDividendDTO createETFDividend(ETFDividendDTO dividendDTO, String userEmail);

    public ETFDividendDTO updateETFDividend(ETFDividendDTO dividendDTO, String userEmail);

    public void deleteETFDividendById(List<Long> ids, String userEmail);

    public void getCSV(String userEmail, Writer writer);

    public void processCSV(String userEmail, MultipartFile file);
}
