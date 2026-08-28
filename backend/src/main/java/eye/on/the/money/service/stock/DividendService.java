package eye.on.the.money.service.stock;

import eye.on.the.money.dto.out.DividendDTO;
import eye.on.the.money.model.Currency;
import eye.on.the.money.model.User;
import eye.on.the.money.model.stock.Dividend;
import eye.on.the.money.model.stock.Stock;
import eye.on.the.money.repository.forex.CurrencyRepository;
import eye.on.the.money.repository.stock.DividendRepository;
import eye.on.the.money.service.shared.ICSVService;
import eye.on.the.money.service.user.UserService;
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
import java.time.LocalDate;
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
    private final StockService stockService;
    private final UserService userService;
    private final ModelMapper modelMapper;
    public List<DividendDTO> getDividends(Long userId) {
        return this.dividendRepository.findByUserIdOrderByDividendDateDesc(userId).stream().map(this::convertToDividendDTO).collect(Collectors.toList());
    }

    public List<DividendDTO> getDividendsBetween(Long userId, LocalDate from, LocalDate to) {
        return this.dividendRepository.findByUserIdAndDividendDateBetweenOrderByDividendDate(userId, from, to)
                .stream().map(this::convertToDividendDTO).collect(Collectors.toList());
    }

    private DividendDTO convertToDividendDTO(Dividend dividend) {
        return this.modelMapper.map(dividend, DividendDTO.class);
    }

    @Transactional
    public DividendDTO createDividend(DividendDTO dividendDTO, Long userId) {
        Currency currency = this.currencyRepository.findById(dividendDTO.getCurrencyId()).orElseThrow(() -> new NoSuchElementException("Currency not found: " + dividendDTO.getCurrencyId()));
        Stock stock = this.stockService.getOrCreateStock(dividendDTO.getShortName(), dividendDTO.getExchange(), dividendDTO.getName());
        User user = this.userService.getReference(userId);

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
    public DividendDTO updateDividend(DividendDTO dividendDTO, Long userId) {
        Currency currency = this.currencyRepository.findById(dividendDTO.getCurrencyId()).orElseThrow(() -> new NoSuchElementException("Currency not found: " + dividendDTO.getCurrencyId()));
        Stock stock = this.stockService.getOrCreateStock(dividendDTO.getShortName(), dividendDTO.getExchange(), dividendDTO.getName());
        Dividend dividend = this.dividendRepository.findByIdAndUserId(dividendDTO.getDividendId(), userId).orElseThrow(() -> new NoSuchElementException("Dividend not found: " + dividendDTO.getDividendId()));

        dividend.setDividendDate(dividendDTO.getDividendDate());
        dividend.setCurrency(currency);
        dividend.setStock(stock);
        dividend.setAmount(dividendDTO.getAmount());

        return this.convertToDividendDTO(dividend);
    }

    @Transactional
    public void deleteDividendById(List<Long> ids, Long userId) {
        this.dividendRepository.deleteByUserIdAndIdIn(userId, ids);
    }

    public void getCSV(Long userId, Writer writer) {
        List<DividendDTO> dividendListList =
                this.dividendRepository.findByUserIdOrderByDividendDate(userId)
                        .stream()
                        .map(this::convertToDividendDTO)
                        .toList();
        this.printRecords(dividendListList, writer);
    }

    @Transactional
    public void processCSV(Long userId, MultipartFile file) {
        long lineNumber = 0;
        try (CSVParser csvParser = this.getParser(file,
                new String[]{"Dividend Id", "Amount", "Dividend Date", "Short Name", "Exchange", "Currency"})) {
            for (CSVRecord csvRecord : csvParser) {
                lineNumber = csvParser.getCurrentLineNumber();
                DividendDTO dividend = DividendDTO.createFromCSVRecord(csvRecord, DateFormats.YYYY_MM_DD);

                if (dividend.getDividendId() != null &&
                        this.dividendRepository.findByIdAndUserId(dividend.getDividendId(), userId).isPresent()) {
                    this.updateDividend(dividend, userId);
                } else {
                    dividend.setDividendId(null);
                    this.createDividend(dividend, userId);
                }
            }
        } catch (IOException | DateTimeParseException | IllegalArgumentException e) {
            log.error("Error while processing CSV", e);
            throw this.csvParseFailure(lineNumber, e);
        }
    }
}
