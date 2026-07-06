package eye.on.the.money.service.stock;

import eye.on.the.money.dto.out.DividendDTO;
import eye.on.the.money.exception.CSVException;
import eye.on.the.money.model.Currency;
import eye.on.the.money.model.User;
import eye.on.the.money.model.stock.Dividend;
import eye.on.the.money.model.stock.Stock;
import eye.on.the.money.repository.forex.CurrencyRepository;
import eye.on.the.money.repository.stock.DividendRepository;
import eye.on.the.money.repository.stock.StockRepository;
import eye.on.the.money.service.shared.ICSVService;
import eye.on.the.money.service.user.UserServiceImpl;
import eye.on.the.money.util.DateFormats;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.Writer;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DividendService implements ICSVService {

    private final DividendRepository dividendRepository;
    private final CurrencyRepository currencyRepository;
    private final StockRepository stockRepository;
    private final UserServiceImpl userService;
    private final ModelMapper modelMapper;
    public List<DividendDTO> getDividends(String userEmail) {
        return this.dividendRepository.findByUserEmailOrderByDividendDate(userEmail).stream().map(this::convertToDividendDTO).collect(Collectors.toList());
    }

    private DividendDTO convertToDividendDTO(Dividend dividend) {
        return this.modelMapper.map(dividend, DividendDTO.class);
    }

    @Transactional
    public DividendDTO createDividend(DividendDTO dividendDTO, String userEmail) {
        Currency currency = this.currencyRepository.findById(dividendDTO.getCurrencyId()).orElseThrow(() -> new NoSuchElementException("Currency not found: " + dividendDTO.getCurrencyId()));
        Stock stock = this.stockRepository.findByShortName(dividendDTO.getShortName()).orElseThrow(() -> new NoSuchElementException("Stock not found: " + dividendDTO.getShortName()));
        User user = this.userService.loadUserByEmail(userEmail);

        Dividend dividend = Dividend.builder()
                .amount(dividendDTO.getAmount())
                .currency(currency)
                .stock(stock)
                .dividendDate(dividendDTO.getDividendDate())
                .user(user)
                .build();

        dividend = this.dividendRepository.save(dividend);
        return this.convertToDividendDTO(dividend);
    }

    @Transactional
    public DividendDTO updateDividend(DividendDTO dividendDTO, String userEmail) {
        Currency currency = this.currencyRepository.findById(dividendDTO.getCurrencyId()).orElseThrow(() -> new NoSuchElementException("Currency not found: " + dividendDTO.getCurrencyId()));
        Stock stock = this.stockRepository.findByShortName(dividendDTO.getShortName()).orElseThrow(() -> new NoSuchElementException("Stock not found: " + dividendDTO.getShortName()));
        Dividend dividend = this.dividendRepository.findByIdAndUserEmail(dividendDTO.getDividendId(), userEmail).orElseThrow(() -> new NoSuchElementException("Dividend not found: " + dividendDTO.getDividendId()));

        dividend.setDividendDate(dividendDTO.getDividendDate());
        dividend.setCurrency(currency);
        dividend.setStock(stock);
        dividend.setAmount(dividendDTO.getAmount());

        return this.convertToDividendDTO(dividend);
    }

    @Transactional
    public void deleteDividendById(List<Long> ids, String userEmail) {
        this.dividendRepository.deleteByUserEmailAndIdIn(userEmail, ids);
    }

    public void getCSV(String userEmail, Writer writer) {
        List<DividendDTO> dividendListList =
                this.dividendRepository.findByUserEmailOrderByDividendDate(userEmail)
                        .stream()
                        .map(this::convertToDividendDTO)
                        .toList();
        this.printRecords(dividendListList, writer);
    }

    @Transactional
    public void processCSV(String userEmail, MultipartFile file) {
        try (CSVParser csvParser = this.getParser(file,
                new String[]{"Dividend Id", "Amount", "Dividend Date", "Short Name", "Exchange", "Currency"})) {
            for (CSVRecord csvRecord : csvParser) {
                DividendDTO dividend = DividendDTO.createFromCSVRecord(csvRecord, DateFormats.YYYY_MM_DD);

                if (dividend.getDividendId() != null &&
                        this.dividendRepository.findByIdAndUserEmail(dividend.getDividendId(), userEmail).isPresent()) {
                    this.updateDividend(dividend, userEmail);
                } else {
                    dividend.setDividendId(null);
                    this.createDividend(dividend, userEmail);
                }
            }
        } catch (IOException | DateTimeParseException | IllegalArgumentException e) {
            log.error("Error while processing CSV", e);
            throw new CSVException("Failed to parse CSV file: " + e.getMessage(), e);
        }
    }
}
