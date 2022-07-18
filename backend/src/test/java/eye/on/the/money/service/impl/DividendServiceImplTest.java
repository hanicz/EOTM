package eye.on.the.money.service.impl;

import eye.on.the.money.EotmApplication;
import eye.on.the.money.dto.out.DividendDTO;
import eye.on.the.money.model.User;
import eye.on.the.money.model.stock.Dividend;
import eye.on.the.money.repository.UserRepository;
import eye.on.the.money.repository.stock.DividendRepository;
import eye.on.the.money.service.DividendService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.StringWriter;
import java.io.Writer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = EotmApplication.class)
class DividendServiceImplTest {

    @Autowired
    private DividendRepository dividendRepository;

    @Autowired
    private DividendService dividendService;

    @Autowired
    private UserRepository userRepository;

    private User user;

    @BeforeEach
    public void init() {
        this.user = this.userRepository.findById(1L).get();
    }

    @Test
    public void getDividends() {
        List<DividendDTO> dividends = this.dividendService.getDividends(1L);
        List<Dividend> dividendsActual = this.dividendRepository.findByUser_IdOrderByDividendDate(1L);
        assertEquals(dividendsActual.size(), dividends.size());
    }

    @Test
    public void getDividends_NoResult() {
        List<DividendDTO> dividends = this.dividendService.getDividends(100L);
        assertEquals(0, dividends.size());
    }

    @Test
    public void createDividend() throws ParseException {
        DividendDTO dividendDTO = this.getDividendDTO();
        dividendDTO.setDividendId(null);
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
        DividendDTO updated = this.dividendService.updateDividend(dividendDTO, this.user);
        assertEquals(dividendDTO, updated);
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
        assertAll(
                () -> assertTrue(writer.toString().contains("Dividend Id,Amount,Dividend Date,Short Name,Currency")),
                () -> assertTrue(writer.toString().contains("1,225.0,2021-06-03 00:00:00.0,MTELEKOM,HUF"))
        );
    }

    @Test
    public void getCSV_Empty() {
        Writer writer = new StringWriter();
        this.dividendService.getCSV(0L, writer);
        assertTrue(writer.toString().isEmpty());
    }

    private DividendDTO getDividendDTO() throws ParseException {
        return DividendDTO.builder()
                .dividendId(1L)
                .amount(10000000.0)
                .dividendDate(new SimpleDateFormat("yyyy-MM-dd").parse("2021-07-03"))
                .shortName("CRSR")
                .currencyId("EUR")
                .build();
    }
}