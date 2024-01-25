package eye.on.the.money.service.stock;

import eye.on.the.money.dto.out.DividendDTO;
import eye.on.the.money.model.Currency;
import eye.on.the.money.model.User;
import eye.on.the.money.model.stock.Dividend;
import eye.on.the.money.model.stock.Stock;
import eye.on.the.money.repository.forex.CurrencyRepository;
import eye.on.the.money.repository.stock.DividendRepository;
import eye.on.the.money.repository.stock.StockRepository;
import eye.on.the.money.service.UserServiceImpl;
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
public class DividendService {
    private final DividendRepository dividendRepository;
    private final CurrencyRepository currencyRepository;
    private final StockRepository stockRepository;
    private final UserServiceImpl userService;
    private final ModelMapper modelMapper;

    @Autowired
    public DividendService(DividendRepository dividendRepository, CurrencyRepository currencyRepository,
                           StockRepository stockRepository, UserServiceImpl userService, ModelMapper modelMapper) {
        this.dividendRepository = dividendRepository;
        this.currencyRepository = currencyRepository;
        this.stockRepository = stockRepository;
        this.userService = userService;
        this.modelMapper = modelMapper;
    }

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
    public void deleteDividendById(List<Long> ids, String userEmail) {
        this.dividendRepository.deleteByUserEmailAndIdIn(userEmail, ids);
    }

    public void getCSV(String userEmail, Writer writer) {
        List<DividendDTO> dividendListList =
                this.dividendRepository.findByUserEmailOrderByDividendDate(userEmail)
                        .stream()
                        .map(this::convertToDividendDTO)
                        .toList();
        try (CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT)) {
            if (!dividendListList.isEmpty()) {
                csvPrinter.printRecord("Dividend Id", "Amount", "Dividend Date", "Short Name", "Currency");
            }
            for (DividendDTO d : dividendListList) {
                csvPrinter.printRecord(d.getDividendId(), d.getAmount(),
                        d.getDividendDate(), d.getShortName(),
                        d.getCurrencyId());
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to create CSV file: " + e.getMessage());
        }
    }

    @Transactional
    public void processCSV(String userEmail, MultipartFile file) {
        try (BufferedReader fileReader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
             CSVParser csvParser = new CSVParser(fileReader, CSVFormat.Builder.create()
                     .setHeader("Dividend Id", "Amount", "Dividend Date", "Short Name", "Currency")
                     .setSkipHeaderRecord(true)
                     .setDelimiter(",")
                     .setTrim(true)
                     .setIgnoreHeaderCase(true)
                     .build())) {

            for (CSVRecord csvRecord : csvParser) {
                String dividendId = csvRecord.get("Dividend Id");
                Date dividendDate = new SimpleDateFormat("yyyy-MM-dd").parse(csvRecord.get("Dividend Date"));

                DividendDTO dividend = DividendDTO.builder()
                        .dividendDate(dividendDate)
                        .amount(Double.parseDouble(csvRecord.get("Amount")))
                        .currencyId(csvRecord.get("Currency"))
                        .shortName(csvRecord.get("Short Name"))
                        .build();

                if (!dividendId.isEmpty() &&
                        this.dividendRepository.findByIdAndUserEmail(Long.parseLong(dividendId), userEmail).isPresent()) {
                    dividend.setDividendId(Long.parseLong(dividendId));
                    this.updateDividend(dividend, userEmail);
                } else {
                    this.createDividend(dividend, userEmail);
                }
            }
        } catch (IOException | ParseException e) {
            throw new RuntimeException("Failed to parse CSV file: " + e.getMessage());
        }
    }
}
