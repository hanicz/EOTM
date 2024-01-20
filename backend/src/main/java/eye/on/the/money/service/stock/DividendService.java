package eye.on.the.money.service.stock;

import eye.on.the.money.dto.out.DividendDTO;
import org.springframework.web.multipart.MultipartFile;

import java.io.Writer;
import java.util.List;

public interface DividendService {
    public List<DividendDTO> getDividends(String userEmail);

    public DividendDTO createDividend(DividendDTO dividendDTO, String userEmail);

    public DividendDTO updateDividend(DividendDTO dividendDTO, String userEmail);

    public void deleteDividendById(List<Long> ids, String userEmail);

    public void processCSV(String userEmail, MultipartFile file);

    public void getCSV(String userEmail, Writer writer);
}
