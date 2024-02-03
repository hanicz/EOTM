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
import eye.on.the.money.service.CSVService;
import eye.on.the.money.service.UserServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.Writer;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class DividendService {

    private final DividendRepository dividendRepository;
    private final CurrencyRepository currencyRepository;
    private final StockRepository stockRepository;
    private final UserServiceImpl userService;
    private final ModelMapper modelMapper;
    private final CSVService csvService;

    private final static DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public List<DividendDTO> getDividends(String userEmail) {
        return this.dividendRepository.findByUserEmailOrderByDividendDate(userEmail).stream().map(this::convertToDividendDTO).collect(Collectors.toList());
    }

    private DividendDTO convertToDividendDTO(Dividend dividend) {
        this.modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.LOOSE);
        return this.modelMapper.map(dividend, DividendDTO.class);
    }

    @Transactional
    public DividendDTO createDividend(DividendDTO dividendDTO, String userEmail) {
        Currency currency = this.currencyRepository.findById(dividendDTO.getCurrencyId()).orElseThrow(NoSuchElementException::new);
        Stock stock = this.stockRepository.findByShortName(dividendDTO.getShortName()).orElseThrow(NoSuchElementException::new);
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
        Currency currency = this.currencyRepository.findById(dividendDTO.getCurrencyId()).orElseThrow(NoSuchElementException::new);
        Stock stock = this.stockRepository.findByShortName(dividendDTO.getShortName()).orElseThrow(NoSuchElementException::new);
        Dividend dividend = this.dividendRepository.findByIdAndUserEmail(dividendDTO.getDividendId(), userEmail).orElseThrow(NoSuchElementException::new);

        dividend.setDividendDate(dividendDTO.getDividendDate());
        dividend.setCurrency(currency);
        dividend.setStock(stock);
        dividend.setAmount(dividendDTO.getAmount());

        return this.convertToDividendDTO(dividend);
    }

    @Transactional
    public void deleteDividendById(String ids, String userEmail) {
        List<Long> idList = Stream.of(ids.split(",")).map(s -> Long.parseLong(s.trim())).collect(Collectors.toList());
        this.dividendRepository.deleteByUserEmailAndIdIn(userEmail, idList);
    }

    public void getCSV(String userEmail, Writer writer) {
        List<DividendDTO> dividendListList =
                this.dividendRepository.findByUserEmailOrderByDividendDate(userEmail)
                        .stream()
                        .map(this::convertToDividendDTO)
                        .toList();
        this.csvService.getCSV(dividendListList, writer);
    }

    @Transactional
    public void processCSV(String userEmail, MultipartFile file) {
        try (CSVParser csvParser = this.csvService.getParser(file,
                new String[]{"Dividend Id", "Amount", "Dividend Date", "Short Name", "Currency"})) {
            for (CSVRecord csvRecord : csvParser) {
                DividendDTO dividend = DividendDTO.createFromCSVRecord(csvRecord, FORMATTER);

                if (dividend.getDividendId() != null &&
                        this.dividendRepository.findByIdAndUserEmail(dividend.getDividendId(), userEmail).isPresent()) {
                    this.updateDividend(dividend, userEmail);
                } else {
                    dividend.setDividendId(null);
                    this.createDividend(dividend, userEmail);
                }
            }
        } catch (IOException | DateTimeParseException e) {
            log.error("Error while processing CSV", e);
            throw new CSVException("Failed to parse CSV file: " + e.getMessage(), e);
        }
    }
}
