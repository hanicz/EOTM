package eye.on.the.money.service;

import eye.on.the.money.dto.out.DividendDTO;
import eye.on.the.money.model.User;
import org.springframework.web.multipart.MultipartFile;

import java.io.Writer;
import java.util.List;

public interface DividendService {
    public List<DividendDTO> getDividends(Long userId);
    public DividendDTO createDividend(DividendDTO dividendDTO, User user);
    public DividendDTO updateDividend(DividendDTO dividendDTO, User user);
    public void deleteDividendById(List<Long> ids);
    public void processCSV(User user, MultipartFile file);
    public void getCSV(Long userId, Writer writer);
}
