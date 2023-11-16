package eye.on.the.money.service.stock.impl;

import eye.on.the.money.EotmApplication;
import eye.on.the.money.model.stock.*;
import eye.on.the.money.repository.stock.StockRepository;
import eye.on.the.money.service.api.EODAPIService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = EotmApplication.class)
@ActiveProfiles("test")
class StockServiceImplTest {

    @MockBean
    private EODAPIService eodAPIService;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private StockServiceImpl stockService;

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
    public void getCandleQuoteByShortName() {
        List<EODCandleQuote> eodList = new ArrayList<>();
        eodList.add(EODCandleQuote.builder().close(1.0).date(new Date()).high(5.0).low(0.2).open(3.5).volume(5123123L).build());
        eodList.add(EODCandleQuote.builder().close(2.0).date(new Date()).high(532.0).low(0.9).open(323.5).volume(5123L).build());
        eodList.add(EODCandleQuote.builder().close(3.0).date(new Date()).high(51.0).low(301.4).open(13.8).volume(7234L).build());
        eodList.add(EODCandleQuote.builder().close(4.0).date(new Date()).high(55.0).low(200.0).open(553.5).volume(94123L).build());
        eodList.add(EODCandleQuote.builder().close(5.0).date(new Date()).high(25.0).low(100.0).open(37.5).volume(7213L).build());

        when(this.eodAPIService.getCandleQuoteByShortName("shortName", 1)).thenReturn(eodList);

        CandleQuote cq = this.stockService.getCandleQuoteByShortName("shortName", 1);

        Assertions.assertAll("Assert all cq arrays",
                () -> assertArrayEquals(eodList.stream().map(EODCandleQuote::getHigh).toArray(Double[]::new), cq.getH()),
                () -> assertArrayEquals(eodList.stream().map(EODCandleQuote::getLow).toArray(Double[]::new), cq.getL()),
                () -> assertArrayEquals(eodList.stream().map(EODCandleQuote::getClose).toArray(Double[]::new), cq.getC()),
                () -> assertArrayEquals(eodList.stream().map(EODCandleQuote::getVolume).toArray(Long[]::new), cq.getV()),
                () -> assertArrayEquals(eodList.stream().map(EODCandleQuote::getOpen).toArray(Double[]::new), cq.getO()),
                () -> assertArrayEquals(eodList.stream().map(ecq -> ecq.getDate().getTime()).toArray(Long[]::new), cq.getT()));
    }
}