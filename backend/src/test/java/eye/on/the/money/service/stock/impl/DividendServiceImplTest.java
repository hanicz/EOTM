package eye.on.the.money.service.stock.impl;

import eye.on.the.money.EotmApplication;
import eye.on.the.money.dto.out.DividendDTO;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = EotmApplication.class)
@ActiveProfiles("test")
class DividendServiceImplTest {

    @Autowired
    private DividendRepository dividendRepository;

    @Autowired
    private DividendServiceImpl dividendService;

    @Autowired
    private UserRepository userRepository;

    private User user;

    private final ModelMapper modelMapper = new ModelMapper();

    @BeforeEach
    public void init() {
        this.user = this.userRepository.findByEmail("test@test.test");
    }

    @Test
    public void getDividends() {
        List<DividendDTO> dividends = this.dividendService.getDividends(this.user.getId());
        List<Dividend> dividendsActual = this.dividendRepository.findByUser_IdOrderByDividendDate(1L);

        Assertions.assertIterableEquals(dividendsActual.stream()
                .map(this::convertToDividendDTO).collect(Collectors.toList()), dividends);
    }

    @Test
    public void getDividends_NoResult() {
        List<DividendDTO> dividends = this.dividendService.getDividends(10000L);
        assertEquals(0, dividends.size());
    }

    @Test
    public void createDividend() throws ParseException {
        DividendDTO dividendDTO = this.getDividendDTO();
        DividendDTO created = this.dividendService.createDividend(dividendDTO, this.user);
        dividendDTO.setDividendId(created.getDividendId());
        assertEquals(dividendDTO, created);
    }

    @Test
    public void createDividend_NoStockFound() throws ParseException {
        DividendDTO dividendDTO = this.getDividendDTO();
        dividendDTO.setShortName("NONEXISTING");
        assertThrows(NoSuchElementException.class,
                () -> this.dividendService.createDividend(dividendDTO, this.user));
    }

    @Test
    public void createDividend_NoCurrencyFound() throws ParseException {
        DividendDTO dividendDTO = this.getDividendDTO();
        dividendDTO.setCurrencyId("NONEXISTING");
        assertThrows(NoSuchElementException.class,
                () -> this.dividendService.createDividend(dividendDTO, this.user));
    }

    @Test
    public void updateDividend() throws ParseException {
        DividendDTO dividendDTO = this.getDividendDTO();
        DividendDTO created = this.dividendService.createDividend(dividendDTO, this.user);
        created.setAmount(111.0);
        DividendDTO updated = this.dividendService.updateDividend(created, this.user);
        assertEquals(created, updated);
    }

    @Test
    public void updateDividend_NoStockFound() throws ParseException {
        DividendDTO dividendDTO = this.getDividendDTO();
        dividendDTO.setShortName("NONEXISTING");
        assertThrows(NoSuchElementException.class,
                () -> this.dividendService.updateDividend(dividendDTO, this.user));
    }

    @Test
    public void updateDividend_NoCurrencyFound() throws ParseException {
        DividendDTO dividendDTO = this.getDividendDTO();
        dividendDTO.setCurrencyId("NONEXISTING");
        assertThrows(NoSuchElementException.class,
                () -> this.dividendService.updateDividend(dividendDTO, this.user));
    }

    @Test
    public void updateDividend_NoDividendFound() throws ParseException {
        DividendDTO dividendDTO = this.getDividendDTO();
        dividendDTO.setDividendId(0L);
        assertThrows(NoSuchElementException.class,
                () -> this.dividendService.updateDividend(dividendDTO, this.user));
    }

    @Test
    public void deleteDividendById() {
        List<Long> ids = new ArrayList<>();
        ids.add(1L);
        this.dividendService.deleteDividendById(ids, this.user);
        Optional<Dividend> dividend = this.dividendRepository.findById(1L);
        assertFalse(dividend.isPresent());
    }

    @Test
    public void getCSV() {
        Writer writer = new StringWriter();
        this.dividendService.getCSV(this.user.getId(), writer);
        System.out.println(writer.toString());
        assertAll(
                () -> assertTrue(writer.toString().contains("Dividend Id,Amount,Dividend Date,Short Name,Currency")),
                () -> assertTrue(writer.toString().contains("2,225.0,2021-08-03 00:00:00.0,CRSR,HUF"))
        );
    }

    @Test
    public void getCSV_Empty() {
        Writer writer = new StringWriter();
        this.dividendService.getCSV(0L, writer);
        assertTrue(writer.toString().isEmpty());
    }

    @Test
    public void processCSV_Update() {
        String csvContent = "Dividend Id,Amount,Dividend Date,Short Name,Currency\n1,250.0,2021-06-03 00:00:00.0,CRSR,HUF";
        MultipartFile mpf = new MockMultipartFile("file", "file.csv", MediaType.TEXT_PLAIN_VALUE, csvContent.getBytes());

        this.dividendService.processCSV(this.user, mpf);

        Dividend updatedDividend = this.dividendRepository.findById(1L).get();

        Assertions.assertEquals(250.0, updatedDividend.getAmount());
    }

    @Test
    public void processCSV_Create() {
        String csvContent = "Dividend Id,Amount,Dividend Date,Short Name,Currency\n,299.0,2021-06-03 00:00:00.0,INTC,USD";
        MultipartFile mpf = new MockMultipartFile("file", "file.csv", MediaType.TEXT_PLAIN_VALUE, csvContent.getBytes());

        this.dividendService.processCSV(this.user, mpf);

        List<Dividend> dividends = this.dividendRepository.findByUser_IdOrderByDividendDate(this.user.getId());

        Optional<Dividend> createdDividend = dividends.stream().filter(d -> d.getAmount() == 299.0 && d.getStock().getId().equals("intc")).findAny();

        Assertions.assertTrue(createdDividend.isPresent());
    }

    @Test
    public void processCSV_Exc() {
        String csvContent = "EXCEPTION,1\n3,EXC,333\n64";
        MultipartFile mpf = new MockMultipartFile("file", "file.csv", MediaType.TEXT_PLAIN_VALUE, csvContent.getBytes());

        assertThrows(RuntimeException.class,
                () -> this.dividendService.processCSV(this.user, mpf));
    }

    private DividendDTO getDividendDTO() throws ParseException {
        return DividendDTO.builder()
                .dividendId(1L)
                .amount(10000000.0)
                .dividendDate(new SimpleDateFormat("yyyy-MM-dd").parse("2021-07-03"))
                .shortName("CRSR")
                .currencyId("EUR")
                .exchange("US")
                .build();
    }

    private DividendDTO convertToDividendDTO(Dividend dividend) {
        this.modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.LOOSE);
        return this.modelMapper.map(dividend, DividendDTO.class);
    }
}