package eye.on.the.money.service.impl;

import eye.on.the.money.dto.out.ETFDividendDTO;
import eye.on.the.money.model.User;
import eye.on.the.money.model.etf.ETF;
import eye.on.the.money.model.etf.ETFDividend;
import eye.on.the.money.model.forex.Currency;
import eye.on.the.money.repository.etf.ETFDividendRepository;
import eye.on.the.money.repository.etf.ETFRepository;
import eye.on.the.money.repository.forex.CurrencyRepository;
import eye.on.the.money.service.ETFDividendService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ETFDividendServiceImpl implements ETFDividendService {

    @Autowired
    private ETFDividendRepository etfDividendRepository;

    @Autowired
    private CurrencyRepository currencyRepository;

    @Autowired
    private ETFRepository etfRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Override
    public List<ETFDividendDTO> getDividends(Long userId) {
        log.trace("Enter getDividends");
        return this.etfDividendRepository.findByUser_IdOrderByDividendDate(userId).stream().map(this::convertToETFDividendDTO).collect(Collectors.toList());
    }

    private ETFDividendDTO convertToETFDividendDTO(ETFDividend dividend) {
        log.trace("Enter convertToETFDividendDTO");
        this.modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.LOOSE);
        return this.modelMapper.map(dividend, ETFDividendDTO.class);
    }

    @Transactional
    @Override
    public ETFDividendDTO createETFDividend(ETFDividendDTO dividendDTO, User user) {
        log.trace("Enter createETFDividend");
        Currency currency = this.currencyRepository.findById(dividendDTO.getCurrencyId()).orElseThrow(NoSuchElementException::new);
        ETF etf = this.etfRepository.findByShortName(dividendDTO.getShortName()).orElseThrow(NoSuchElementException::new);

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
    @Override
    public ETFDividendDTO updateETFDividend(ETFDividendDTO dividendDTO, User user) {
        log.trace("Enter updateETFDividend");
        Currency currency = this.currencyRepository.findById(dividendDTO.getCurrencyId()).orElseThrow(NoSuchElementException::new);
        ETF etf = this.etfRepository.findByShortName(dividendDTO.getShortName()).orElseThrow(NoSuchElementException::new);
        ETFDividend dividend = this.etfDividendRepository.findByIdAndUser_Id(dividendDTO.getId(), user.getId()).orElseThrow(NoSuchElementException::new);

        dividend.setDividendDate(dividendDTO.getDividendDate());
        dividend.setCurrency(currency);
        dividend.setEtf(etf);
        dividend.setAmount(dividendDTO.getAmount());

        return this.convertToETFDividendDTO(dividend);
    }

    @Transactional
    @Override
    public void deleteETFDividendById(List<Long> ids, User user) {
        log.trace("Enter deleteETFDividendById");
        this.etfDividendRepository.deleteByUser_idAndIdIn(user.getId(), ids);
    }

    @Override
    public void getCSV(Long userId, Writer writer) {
        List<ETFDividendDTO> dividendListList =
                this.etfDividendRepository.findByUser_IdOrderByDividendDate(userId)
                        .stream()
                        .map(this::convertToETFDividendDTO).
                        collect(Collectors.toList());
        try (CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT)) {
            if (!dividendListList.isEmpty()) {
                csvPrinter.printRecord("Dividend Id", "Amount", "Dividend Date", "Short Name", "Currency");
            }
            for (ETFDividendDTO d : dividendListList) {
                csvPrinter.printRecord(d.getId(), d.getAmount(),
                        d.getDividendDate(), d.getShortName(),
                        d.getCurrencyId());
            }
        } catch (IOException e) {
            log.error("Error while writing to CSV", e);
            throw new RuntimeException("fail to create CSV file: " + e.getMessage());
        }
    }

    @Transactional
    @Override
    public void processCSV(User user, MultipartFile file) {
        try (BufferedReader fileReader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
             CSVParser csvParser = new CSVParser(fileReader, CSVFormat.Builder.create()
                     .setHeader("Dividend Id", "Amount", "Dividend Date", "Short Name", "Currency")
                     .setSkipHeaderRecord(true)
                     .setDelimiter(",")
                     .setTrim(true)
                     .setIgnoreHeaderCase(true)
                     .build())) {

            Iterable<CSVRecord> csvRecords = csvParser.getRecords();
            for (CSVRecord csvRecord : csvRecords) {
                Date dividendDate = new SimpleDateFormat("yyyy-MM-dd").parse(csvRecord.get("Dividend Date"));

                ETFDividendDTO dividend = ETFDividendDTO.builder()
                        .dividendDate(dividendDate)
                        .amount(Double.parseDouble(csvRecord.get("Amount")))
                        .currencyId(csvRecord.get("Currency"))
                        .shortName(csvRecord.get("Short Name"))
                        .build();

                if (!("").equals(csvRecord.get("Dividend Id")) &&
                        this.etfDividendRepository.findByIdAndUser_Id(Long.parseLong(csvRecord.get("Dividend Id")), user.getId()).isPresent()) {
                    dividend.setId(Long.parseLong(csvRecord.get("Dividend Id")));
                    this.updateETFDividend(dividend, user);
                } else {
                    this.createETFDividend(dividend, user);
                }
            }
        } catch (IOException | ParseException e) {
            log.error("Error while processing CSV", e);
            throw new RuntimeException("fail to parse CSV file: " + e.getMessage());
        }
    }
}
