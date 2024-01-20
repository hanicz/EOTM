package eye.on.the.money.service;

import eye.on.the.money.dto.in.TaxEntry;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface TaxService {
    void loadRatesFromXLS(MultipartFile file);
    void doTaxByYear(String userEmail, Integer year, List<TaxEntry> taxEntries);
}
