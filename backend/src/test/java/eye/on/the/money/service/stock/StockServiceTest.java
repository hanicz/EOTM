package eye.on.the.money.service.stock;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eye.on.the.money.EotmApplication;
import eye.on.the.money.model.stock.*;
import eye.on.the.money.repository.stock.StockRepository;
import eye.on.the.money.service.api.EODAPIService;
import eye.on.the.money.service.stock.StockService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = EotmApplication.class)
@ActiveProfiles("test")
class StockServiceTest {

    @MockBean
    private EODAPIService eodAPIService;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private StockService stockService;

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void getAllStocks() {
        List<Stock> stocks = this.stockRepository.findAllByOrderByShortNameAsc();
        List<Stock> result = this.stockService.getAllStocks();

        assertEquals(stocks, result);
    }

    @Test
    public void getAllSymbols() {
        List<Symbol> symbols = new ArrayList<>();
        symbols.add(Symbol.builder().name("N1").code("C1").type("T1").build());
        symbols.add(Symbol.builder().name("N2").code("C2").type("T2").build());
        symbols.add(Symbol.builder().name("N3").code("C3").type("T3").build());

        when(this.eodAPIService.getAllSymbols("exchange")).thenReturn(symbols);

        List<Symbol> result = this.stockService.getAllSymbols("exchange");

        assertEquals(symbols, result);
    }

    @Test
    public void getAllExchanges() {
        List<Exchange> exchanges = new ArrayList<>();
        exchanges.add(Exchange.builder().name("N1").code("C1").currency("CU1").build());
        exchanges.add(Exchange.builder().name("N2").code("C2").currency("CU2").build());
        exchanges.add(Exchange.builder().name("N3").code("C3").currency("CU3").build());

        when(this.eodAPIService.getAllExchanges()).thenReturn(exchanges);

        List<Exchange> result = this.stockService.getAllExchanges();

        assertEquals(exchanges, result);
    }

    @Test
    public void getCandleQuoteByShortNameSameDay() throws JsonProcessingException {
        List<EODCandleQuote> eodList = this.geteodList();

        when(this.eodAPIService.getCandleQuoteByShortName("shortName", 1)).thenReturn(eodList);
        when(this.eodAPIService.getLiveValueForSingle("shortName", "/real-time/{1}/?api_token={0}&fmt=json&"))
                .thenReturn(mapper.readTree("{\"code\":\"AMD.US\",\"timestamp\":" + TimeUnit.MILLISECONDS.toSeconds(new Date().getTime()) + ",\"gmtoffset\":0,\"open\":119.64,\"high\":119.97,\"low\":118.82,\"close\":119.7044,\"volume\":5094170,\"previousClose\":119.83,\"change\":-0.1256,\"change_p\":-0.1048}"));

        CandleQuote cq = this.stockService.getCandleQuoteByShortName("shortName", 1);

        Assertions.assertAll("Assert all cq arrays",
                () -> assertArrayEquals(eodList.stream().map(EODCandleQuote::getHigh).toArray(Double[]::new), cq.getH()),
                () -> assertArrayEquals(eodList.stream().map(EODCandleQuote::getLow).toArray(Double[]::new), cq.getL()),
                () -> assertArrayEquals(eodList.stream().map(EODCandleQuote::getClose).toArray(Double[]::new), cq.getC()),
                () -> assertArrayEquals(eodList.stream().map(EODCandleQuote::getVolume).toArray(Long[]::new), cq.getV()),
                () -> assertArrayEquals(eodList.stream().map(EODCandleQuote::getOpen).toArray(Double[]::new), cq.getO()),
                () -> assertArrayEquals(eodList.stream().map(ecq -> ecq.getDate().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()).toArray(Long[]::new), cq.getT()));
    }

    @Test
    public void getCandleQuoteByShortNameNotSameDay() throws JsonProcessingException {
        List<EODCandleQuote> eodList = this.geteodList();

        when(this.eodAPIService.getCandleQuoteByShortName("shortName", 1)).thenReturn(eodList);
        when(this.eodAPIService.getLiveValueForSingle("shortName", "/real-time/{1}/?api_token={0}&fmt=json&"))
                .thenReturn(mapper.readTree("{\"code\":\"AMD.US\",\"timestamp\": 1700255640,\"gmtoffset\":0,\"open\":119.64,\"high\":119.97,\"low\":118.82,\"close\":119.7044,\"volume\":5094170,\"previousClose\":119.83,\"change\":-0.1256,\"change_p\":-0.1048}"));

        CandleQuote cq = this.stockService.getCandleQuoteByShortName("shortName", 1);

        eodList.add(EODCandleQuote.builder().close(119.7044).date(Instant.ofEpochSecond(1700255640).atZone(ZoneId.systemDefault()).toLocalDate()).high(119.97).low(118.82).open(119.64).volume(5094170L).build());

        Assertions.assertAll("Assert all cq arrays",
                () -> assertArrayEquals(eodList.stream().map(EODCandleQuote::getHigh).toArray(Double[]::new), cq.getH()),
                () -> assertArrayEquals(eodList.stream().map(EODCandleQuote::getLow).toArray(Double[]::new), cq.getL()),
                () -> assertArrayEquals(eodList.stream().map(EODCandleQuote::getClose).toArray(Double[]::new), cq.getC()),
                () -> assertArrayEquals(eodList.stream().map(EODCandleQuote::getVolume).toArray(Long[]::new), cq.getV()),
                () -> assertArrayEquals(eodList.stream().map(EODCandleQuote::getOpen).toArray(Double[]::new), cq.getO()),
                () -> assertArrayEquals(eodList.stream().map(ecq -> ecq.getDate().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()).toArray(Long[]::new), cq.getT()));
    }

    @Test
    public void getOrCreateStockExist() {
        Stock expected = this.stockRepository.findById("crsr").get();
        Stock result = this.stockService.getOrCreateStock("crsr", "US", "Corsair Gaming Inc");

        Assertions.assertEquals(expected, result);
    }

    @Test
    public void getOrCreateStockNew() {
        Optional<Stock> empty = this.stockRepository.findById("new");
        Stock result = this.stockService.getOrCreateStock("new", "US", "New Stock Test");

        Stock expected = this.stockRepository.findById("new").get();

        Assertions.assertTrue(empty.isEmpty());
        Assertions.assertEquals(expected, result);
    }

    private List<EODCandleQuote> geteodList(){
        List<EODCandleQuote> eodList = new ArrayList<>();
        eodList.add(EODCandleQuote.builder().close(1.0).date(LocalDate.now()).high(5.0).low(0.2).open(3.5).volume(5123123L).build());
        eodList.add(EODCandleQuote.builder().close(2.0).date(LocalDate.now()).high(532.0).low(0.9).open(323.5).volume(5123L).build());
        eodList.add(EODCandleQuote.builder().close(3.0).date(LocalDate.now()).high(51.0).low(301.4).open(13.8).volume(7234L).build());
        eodList.add(EODCandleQuote.builder().close(4.0).date(LocalDate.now()).high(55.0).low(200.0).open(553.5).volume(94123L).build());
        eodList.add(EODCandleQuote.builder().close(5.0).date(LocalDate.now()).high(25.0).low(100.0).open(37.5).volume(7213L).build());

        return eodList;
    }
}