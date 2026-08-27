package eye.on.the.money.service.stock;

import eye.on.the.money.EotmApplication;
import eye.on.the.money.dto.out.DividendDTO;
import eye.on.the.money.exception.CSVException;
import eye.on.the.money.model.User;
import eye.on.the.money.model.stock.Dividend;
import eye.on.the.money.repository.UserRepository;
import eye.on.the.money.repository.stock.DividendRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.multipart.MultipartFile;

import java.io.StringWriter;
import java.io.Writer;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = EotmApplication.class)
@ActiveProfiles("test")
class DividendServiceTest {

    @Autowired
    private DividendRepository dividendRepository;

    @Autowired
    private DividendService dividendService;

    @Autowired
    private UserRepository userRepository;

    private User user;

    @Autowired
    private ModelMapper modelMapper;

    private final static DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @BeforeEach
    public void init() {
        this.user = this.userRepository.findByEmail("test@test.test");
    }

    @Test
    public void getDividends() {
        List<DividendDTO> dividends = this.dividendService.getDividends(this.user.getId());
        List<Dividend> dividendsActual = this.dividendRepository.findByUserIdOrderByDividendDateDesc(this.user.getId());

        Assertions.assertIterableEquals(dividendsActual.stream()
                .map(this::convertToDividendDTO).collect(Collectors.toList()), dividends);
    }

    @Test
    public void getDividends_NoResult() {
        List<DividendDTO> dividends = this.dividendService.getDividends(-1L);
        assertEquals(0, dividends.size());
    }

    @Test
    public void createDividend() throws ParseException {
        DividendDTO dividendDTO = this.getDividendDTO();
        DividendDTO created = this.dividendService.createDividend(dividendDTO, this.user.getId());
        dividendDTO.setDividendId(created.getDividendId());
        assertEquals(dividendDTO, created);
    }

    @Test
    public void createDividend_CreatesMissingStock() throws ParseException {
        DividendDTO dividendDTO = this.getDividendDTO();
        dividendDTO.setShortName("NEWTICKER");
        dividendDTO.setName("New Ticker Inc");
        DividendDTO created = this.dividendService.createDividend(dividendDTO, this.user.getId());
        assertEquals("NEWTICKER", created.getShortName());
        assertEquals("New Ticker Inc", created.getName());
    }

    @Test
    public void createDividend_NoCurrencyFound() throws ParseException {
        DividendDTO dividendDTO = this.getDividendDTO();
        dividendDTO.setCurrencyId("NONEXISTING");
        assertThrows(NoSuchElementException.class,
                () -> this.dividendService.createDividend(dividendDTO, this.user.getId()));
    }

    @Test
    public void updateDividend() throws ParseException {
        DividendDTO dividendDTO = this.getDividendDTO();
        DividendDTO created = this.dividendService.createDividend(dividendDTO, this.user.getId());
        created.setAmount(111.0);
        DividendDTO updated = this.dividendService.updateDividend(created, this.user.getId());
        assertEquals(created, updated);
    }

    @Test
    public void updateDividend_CreatesMissingStock() throws ParseException {
        DividendDTO created = this.dividendService.createDividend(this.getDividendDTO(), this.user.getId());
        created.setShortName("OTHERTICKER");
        created.setName("Other Ticker Inc");
        DividendDTO updated = this.dividendService.updateDividend(created, this.user.getId());
        assertEquals("OTHERTICKER", updated.getShortName());
        assertEquals("Other Ticker Inc", updated.getName());
    }

    @Test
    public void updateDividend_NoCurrencyFound() throws ParseException {
        DividendDTO dividendDTO = this.getDividendDTO();
        dividendDTO.setCurrencyId("NONEXISTING");
        assertThrows(NoSuchElementException.class,
                () -> this.dividendService.updateDividend(dividendDTO, this.user.getId()));
    }

    @Test
    public void updateDividend_NoDividendFound() throws ParseException {
        DividendDTO dividendDTO = this.getDividendDTO();
        dividendDTO.setDividendId(0L);
        assertThrows(NoSuchElementException.class,
                () -> this.dividendService.updateDividend(dividendDTO, this.user.getId()));
    }

    @Test
    public void deleteDividendById() {
        this.dividendService.deleteDividendById(List.of(1L), this.user.getId());
        Optional<Dividend> dividend = this.dividendRepository.findById(1L);
        assertFalse(dividend.isPresent());
    }

    @Test
    public void getCSV() {
        Writer writer = new StringWriter();
        this.dividendService.getCSV(this.user.getId(), writer);
        assertAll(
                () -> assertTrue(writer.toString().contains("Dividend Id,Amount,Dividend Date,Short Name,Exchange,Currency")),
                () -> assertTrue(writer.toString().contains("2,225.0,2021-08-03,CRSR,US,HUF"))
        );
    }

    @Test
    public void getCSV_Empty() {
        Writer writer = new StringWriter();
        this.dividendService.getCSV(-1L, writer);
        assertTrue(writer.toString().isEmpty());
    }

    @Test
    public void processCSV_Update() {
        String csvContent = "Dividend Id,Amount,Dividend Date,Short Name,Exchange,Currency\n1,250.0,2021-06-03,CRSR,US,HUF";
        MultipartFile mpf = new MockMultipartFile("file", "file.csv", MediaType.TEXT_PLAIN_VALUE, csvContent.getBytes());

        this.dividendService.processCSV(this.user.getId(), mpf);

        Dividend updatedDividend = this.dividendRepository.findById(1L).get();

        Assertions.assertEquals(250.0, updatedDividend.getAmount());
    }

    @Test
    public void processCSV_Create() {
        String csvContent = "Dividend Id,Amount,Dividend Date,Short Name,Exchange,Currency\n,299.0,2021-06-03,INTC,US,USD";
        MultipartFile mpf = new MockMultipartFile("file", "file.csv", MediaType.TEXT_PLAIN_VALUE, csvContent.getBytes());

        this.dividendService.processCSV(this.user.getId(), mpf);

        List<Dividend> dividends = this.dividendRepository.findByUserIdOrderByDividendDate(this.user.getId());

        Optional<Dividend> createdDividend = dividends.stream().filter(d -> d.getAmount() == 299.0 && d.getStock().getId().equals("intc.us")).findAny();

        Assertions.assertTrue(createdDividend.isPresent());
    }

    @Test
    public void processCSV_Exc() {
        String csvContent = "EXCEPTION,1\n3,EXC,333\n64";
        MultipartFile mpf = new MockMultipartFile("file", "file.csv", MediaType.TEXT_PLAIN_VALUE, csvContent.getBytes());

        assertThrows(CSVException.class,
                () -> this.dividendService.processCSV(this.user.getId(), mpf));
    }

    @Test
    public void processCSV_DateExc() {
        String csvContent = "Dividend Id,Amount,Dividend Date,Short Name,Exchange,Currency\n,299.0,NOT_DATE,INTC,US,USD";
        MultipartFile mpf = new MockMultipartFile("file", "file.csv", MediaType.TEXT_PLAIN_VALUE, csvContent.getBytes());

        assertThrows(CSVException.class,
                () -> this.dividendService.processCSV(this.user.getId(), mpf));
    }

    private DividendDTO getDividendDTO() throws ParseException {
        return DividendDTO.builder()
                .dividendId(1L)
                .amount(10000000.0)
                .dividendDate(LocalDate.parse("2021-07-03", FORMATTER))
                .shortName("CRSR")
                .name("Corsair Gaming Inc")
                .currencyId("EUR")
                .exchange("US")
                .build();
    }

    private DividendDTO convertToDividendDTO(Dividend dividend) {
        this.modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.LOOSE);
        return this.modelMapper.map(dividend, DividendDTO.class);
    }
}