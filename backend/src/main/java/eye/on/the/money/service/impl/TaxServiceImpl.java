package eye.on.the.money.service.impl;

import eye.on.the.money.dto.in.TaxEntry;
import eye.on.the.money.dto.out.InvestmentDTO;
import eye.on.the.money.model.forex.Currency;
import eye.on.the.money.model.tax.MNBRate;
import eye.on.the.money.repository.forex.CurrencyRepository;
import eye.on.the.money.repository.tax.MNBRateRepository;
import eye.on.the.money.service.InvestmentService;
import eye.on.the.money.service.TaxService;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
public class TaxServiceImpl implements TaxService {

    @Autowired
    private MNBRateRepository mnbRateRepository;

    @Autowired
    private CurrencyRepository currencyRepository;

    @Autowired
    private InvestmentService investmentService;


    @Override
    public void loadRatesFromXLS(MultipartFile file) {
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet rateSheet = workbook.getSheetAt(0);
            Map<Currency, Integer> rateMap = this.processHeaderRow(rateSheet.getRow(0));
            for (Row row : rateSheet) {
                if (row.getRowNum() < 2) continue;

                for (Map.Entry<Currency, Integer> entry : rateMap.entrySet()) {
                    Date rateDateInExcel = row.getCell(0).getDateCellValue();
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

    private void createNewRateEntry(Currency currency, Date rateDate, Double rate) {
        MNBRate mnbRate = MNBRate.builder()
                .rate(rate)
                .rateDate(rateDate)
                .currency(currency)
                .build();
        this.mnbRateRepository.save(mnbRate);
    }


    @Override
    public void doTaxByYear(Long userId, Integer year, List<TaxEntry> taxEntries) {
        List<InvestmentDTO> investments = this.investmentService.getInvestmentsByTypeAndDate(userId, "S", this.getDate(year, 0), this.getDate(year, 11));
        List<Long> investmentIds = taxEntries.stream().map(TaxEntry::getInvestmentId).collect(Collectors.toList());


    }

    private Date getDate(Integer year, Integer month) {
        Calendar calendarStart = Calendar.getInstance();
        calendarStart.set(Calendar.YEAR, year);
        calendarStart.set(Calendar.MONTH, month);
        calendarStart.set(Calendar.DAY_OF_MONTH, 31);
        return calendarStart.getTime();
    }
}
