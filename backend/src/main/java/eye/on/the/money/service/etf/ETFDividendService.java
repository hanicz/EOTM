package eye.on.the.money.service.etf;

import eye.on.the.money.dto.out.ETFDividendDTO;
import eye.on.the.money.exception.CSVException;
import eye.on.the.money.model.Currency;
import eye.on.the.money.model.User;
import eye.on.the.money.model.etf.ETF;
import eye.on.the.money.model.etf.ETFDividend;
import eye.on.the.money.repository.etf.ETFDividendRepository;
import eye.on.the.money.repository.etf.ETFRepository;
import eye.on.the.money.repository.forex.CurrencyRepository;
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
@Slf4j
@RequiredArgsConstructor
public class ETFDividendService {

    private final ETFDividendRepository etfDividendRepository;
    private final CurrencyRepository currencyRepository;
    private final ETFRepository etfRepository;
    private final UserServiceImpl userService;
    private final ModelMapper modelMapper;
    private final CSVService csvService;

    private final static DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public List<ETFDividendDTO> getDividends(String userEmail) {
        log.trace("Enter getDividends");
        return this.etfDividendRepository.findByUserEmailOrderByDividendDate(userEmail).stream().map(this::convertToETFDividendDTO).collect(Collectors.toList());
    }

    private ETFDividendDTO convertToETFDividendDTO(ETFDividend dividend) {
        log.trace("Enter convertToETFDividendDTO");
        this.modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.LOOSE);
        return this.modelMapper.map(dividend, ETFDividendDTO.class);
    }

    @Transactional
    public ETFDividendDTO createETFDividend(ETFDividendDTO dividendDTO, String userEmail) {
        log.trace("Enter createETFDividend");
        Currency currency = this.currencyRepository.findById(dividendDTO.getCurrencyId()).orElseThrow(NoSuchElementException::new);
        ETF etf = this.etfRepository.findByShortName(dividendDTO.getShortName()).orElseThrow(NoSuchElementException::new);
        User user = this.userService.loadUserByEmail(userEmail);

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
    public ETFDividendDTO updateETFDividend(ETFDividendDTO dividendDTO, String userEmail) {
        log.trace("Enter updateETFDividend");
        Currency currency = this.currencyRepository.findById(dividendDTO.getCurrencyId()).orElseThrow(NoSuchElementException::new);
        ETF etf = this.etfRepository.findByShortName(dividendDTO.getShortName()).orElseThrow(NoSuchElementException::new);
        ETFDividend dividend = this.etfDividendRepository.findByIdAndUserEmail(dividendDTO.getId(), userEmail).orElseThrow(NoSuchElementException::new);

        dividend.setDividendDate(dividendDTO.getDividendDate());
        dividend.setCurrency(currency);
        dividend.setEtf(etf);
        dividend.setAmount(dividendDTO.getAmount());

        return this.convertToETFDividendDTO(dividend);
    }

    @Transactional
    public void deleteETFDividendById(String ids, String userEmail) {
        log.trace("Enter deleteETFDividendById");
        List<Long> idList = Stream.of(ids.split(",")).map(s -> Long.parseLong(s.trim())).collect(Collectors.toList());
        this.etfDividendRepository.deleteByUserEmailAndIdIn(userEmail, idList);
    }

    public void getCSV(String userEmail, Writer writer) {
        List<ETFDividendDTO> dividendListList =
                this.etfDividendRepository.findByUserEmailOrderByDividendDate(userEmail)
                        .stream()
                        .map(this::convertToETFDividendDTO)
                        .toList();
        this.csvService.getCSV(dividendListList, writer);
    }

    @Transactional
    public void processCSV(String userEmail, MultipartFile file) {
        try (CSVParser csvParser = this.csvService.getParser(file,
                new String[]{"Dividend Id", "Amount", "Dividend Date", "Short Name", "Currency"})) {
            for (CSVRecord csvRecord : csvParser) {
                ETFDividendDTO dividend = ETFDividendDTO.createFromCSVRecord(csvRecord, FORMATTER);

                if (dividend.getId() != null &&
                        this.etfDividendRepository.findByIdAndUserEmail(dividend.getId(), userEmail).isPresent()) {
                    this.updateETFDividend(dividend, userEmail);
                } else {
                    dividend.setId(null);
                    this.createETFDividend(dividend, userEmail);
                }
            }
        } catch (IOException | DateTimeParseException e) {
            log.error("Error while processing CSV", e);
            throw new CSVException("Failed to parse CSV file: " + e.getMessage(), e);
        }
    }
}
