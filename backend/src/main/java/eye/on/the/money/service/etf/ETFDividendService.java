package eye.on.the.money.service.etf;

import eye.on.the.money.dto.out.ETFDividendDTO;
import eye.on.the.money.model.Currency;
import eye.on.the.money.model.User;
import eye.on.the.money.model.etf.ETF;
import eye.on.the.money.model.etf.ETFDividend;
import eye.on.the.money.repository.etf.ETFDividendRepository;
import eye.on.the.money.repository.forex.CurrencyRepository;
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
@Slf4j
@RequiredArgsConstructor
public class ETFDividendService implements ICSVService {

    private final ETFDividendRepository etfDividendRepository;
    private final CurrencyRepository currencyRepository;
    private final ETFService etfService;
    private final UserService userService;
    private final ModelMapper modelMapper;
    public List<ETFDividendDTO> getDividends(Long userId) {
        return this.etfDividendRepository.findByUserIdOrderByDividendDateDesc(userId).stream().map(this::convertToETFDividendDTO).collect(Collectors.toList());
    }

    public List<ETFDividendDTO> getDividendsBetween(Long userId, LocalDate from, LocalDate to) {
        return this.etfDividendRepository.findByUserIdAndDividendDateBetweenOrderByDividendDate(userId, from, to)
                .stream().map(this::convertToETFDividendDTO).collect(Collectors.toList());
    }

    private ETFDividendDTO convertToETFDividendDTO(ETFDividend dividend) {
        return this.modelMapper.map(dividend, ETFDividendDTO.class);
    }

    @Transactional
    public ETFDividendDTO createETFDividend(ETFDividendDTO dividendDTO, Long userId) {
        Currency currency = this.currencyRepository.findById(dividendDTO.getCurrencyId()).orElseThrow(() -> new NoSuchElementException("Currency not found: " + dividendDTO.getCurrencyId()));
        ETF etf = this.etfService.getOrCreateETF(dividendDTO.getShortName(), dividendDTO.getExchange(), dividendDTO.getName());
        User user = this.userService.getReference(userId);

        ETFDividend dividend = ETFDividend.builder()
                .amount(dividendDTO.getAmount())
                .currency(currency)
                .etf(etf)
                .dividendDate(dividendDTO.getDividendDate())
                .user(user)
                .build();

        dividend = this.etfDividendRepository.save(dividend);
        return this.convertToETFDividendDTO(dividend);
    }

    @Transactional
    public ETFDividendDTO updateETFDividend(ETFDividendDTO dividendDTO, Long userId) {
        Currency currency = this.currencyRepository.findById(dividendDTO.getCurrencyId()).orElseThrow(() -> new NoSuchElementException("Currency not found: " + dividendDTO.getCurrencyId()));
        ETF etf = this.etfService.getOrCreateETF(dividendDTO.getShortName(), dividendDTO.getExchange(), dividendDTO.getName());
        ETFDividend dividend = this.etfDividendRepository.findByIdAndUserId(dividendDTO.getId(), userId).orElseThrow(() -> new NoSuchElementException("ETF dividend not found: " + dividendDTO.getId()));

        dividend.setDividendDate(dividendDTO.getDividendDate());
        dividend.setCurrency(currency);
        dividend.setEtf(etf);
        dividend.setAmount(dividendDTO.getAmount());

        return this.convertToETFDividendDTO(dividend);
    }

    @Transactional
    public void deleteETFDividendById(List<Long> ids, Long userId) {
        this.etfDividendRepository.deleteByUserIdAndIdIn(userId, ids);
    }

    public void getCSV(Long userId, Writer writer) {
        List<ETFDividendDTO> dividendListList =
                this.etfDividendRepository.findByUserIdOrderByDividendDate(userId)
                        .stream()
                        .map(this::convertToETFDividendDTO)
                        .toList();
        this.printRecords(dividendListList, writer);
    }

    @Transactional
    public void processCSV(Long userId, MultipartFile file) {
        long lineNumber = 0;
        try (CSVParser csvParser = this.getParser(file,
                new String[]{"Dividend Id", "Amount", "Dividend Date", "Short Name","Exchange", "Currency"})) {
            for (CSVRecord csvRecord : csvParser) {
                lineNumber = csvParser.getCurrentLineNumber();
                ETFDividendDTO dividend = ETFDividendDTO.createFromCSVRecord(csvRecord, DateFormats.YYYY_MM_DD);

                if (dividend.getId() != null &&
                        this.etfDividendRepository.findByIdAndUserId(dividend.getId(), userId).isPresent()) {
                    this.updateETFDividend(dividend, userId);
                } else {
                    dividend.setId(null);
                    this.createETFDividend(dividend, userId);
                }
            }
        } catch (IOException | DateTimeParseException | IllegalArgumentException e) {
            log.error("Error while processing CSV", e);
            throw this.csvParseFailure(lineNumber, e);
        }
    }
}
