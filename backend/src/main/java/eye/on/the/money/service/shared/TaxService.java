package eye.on.the.money.service.shared;

import eye.on.the.money.model.Currency;
import eye.on.the.money.model.tax.MNBRate;
import eye.on.the.money.repository.forex.CurrencyRepository;
import eye.on.the.money.repository.tax.MNBRateRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.StreamSupport;

@Service
@RequiredArgsConstructor
public class TaxService {

    private final MNBRateRepository mnbRateRepository;
    private final CurrencyRepository currencyRepository;

    public void loadRatesFromXLS(MultipartFile file) {
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet rateSheet = workbook.getSheetAt(0);
            Map<Currency, Integer> rateMap = this.processHeaderRow(rateSheet.getRow(0));
            for (Row row : rateSheet) {
                if (row.getRowNum() < 2) continue;

                for (Map.Entry<Currency, Integer> entry : rateMap.entrySet()) {
                    LocalDate rateDateInExcel = row.getCell(0).getLocalDateTimeCellValue().toLocalDate();
                    Double rateInExcel = row.getCell(entry.getValue()).getNumericCellValue();
                    Optional<MNBRate> rate = this.mnbRateRepository.findByCurrency_IdAndRateDate(entry.getKey().getId(), rateDateInExcel);
                    rate.ifPresentOrElse(r -> r.setRate(rateInExcel),
                            () -> this.createNewRateEntry(entry.getKey(), rateDateInExcel, rateInExcel));
                }
            }
        } catch (IOException e) {
        }
    }

    private Map<Currency, Integer> processHeaderRow(Row headerRow) {
        Iterable<Currency> currencies = this.currencyRepository.findAll();
        Map<Currency, Integer> rateMap = new HashMap<>();
        for (Cell cell : headerRow) {
            if (!"HUF".equals(cell.getStringCellValue()) &&
                    StreamSupport.stream(currencies.spliterator(), false).anyMatch(currency -> currency.getId().equals(cell.getStringCellValue()))) {
                rateMap.put(this.currencyRepository.findById(cell.getStringCellValue()).get(), cell.getColumnIndex());
            }
        }
        return rateMap;
    }

    private void createNewRateEntry(Currency currency, LocalDate rateDate, Double rate) {
        MNBRate mnbRate = MNBRate.builder()
                .rate(rate)
                .rateDate(rateDate)
                .currency(currency)
                .build();
        this.mnbRateRepository.save(mnbRate);
    }
}
