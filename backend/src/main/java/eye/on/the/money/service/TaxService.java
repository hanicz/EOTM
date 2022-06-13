package eye.on.the.money.service;

import eye.on.the.money.dto.in.TaxEntry;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface TaxService {
    public void loadRatesFromXLS(MultipartFile file);
    public void doTaxByYear(Long userId, Integer year, List<TaxEntry> taxEntries);
}
