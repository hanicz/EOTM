package eye.on.the.money.controller;

import eye.on.the.money.dto.out.CandleQuoteDTO;
import eye.on.the.money.model.stock.Exchange;
import eye.on.the.money.model.stock.Stock;
import eye.on.the.money.model.stock.Symbol;
import eye.on.the.money.service.stock.StockService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class StockControllerTest {

    @Mock
    private StockService stockService;

    @InjectMocks
    private StockController stockController;

    @Test
    void getAllExchanges() {
        List<Exchange> exchanges = new ArrayList<>();
        exchanges.add(Exchange.builder().code("c1").currency("cc1").name("n1").build());
        exchanges.add(Exchange.builder().code("c2").currency("cc2").name("n2").build());
        exchanges.add(Exchange.builder().code("c3").currency("cc3").name("n3").build());
        when(this.stockService.getAllExchanges()).thenReturn(exchanges);

        Assertions.assertIterableEquals(exchanges, stockController.getAllExchanges().getBody());
    }

    @Test
    void getAllStocks() {
        List<Stock> stocks = new ArrayList<>();
        stocks.add(Stock.builder().id("s1").name("n1").shortName("sn1").exchange("e1").build());
        stocks.add(Stock.builder().id("s2").name("n2").shortName("sn2").exchange("e2").build());
        stocks.add(Stock.builder().id("s3").name("n3").shortName("sn3").exchange("e3").build());
        when(this.stockService.getAllStocks()).thenReturn(stocks);

        Assertions.assertIterableEquals(stocks, stockController.getAllStocks().getBody());
    }

    @Test
    void getAllSymbols() {
        List<Symbol> symbols = new ArrayList<>();
        symbols.add(Symbol.builder().code("c1").name("n1").type("t1").build());
        symbols.add(Symbol.builder().code("c2").name("n2").type("t2").build());
        symbols.add(Symbol.builder().code("c3").name("n3").type("t3").build());
        when(this.stockService.getAllSymbols("exchange")).thenReturn(symbols);

        Assertions.assertIterableEquals(symbols, stockController.getAllSymbols("exchange").getBody());
    }

    @Test
    void getCandleQuoteByShortName() {
        CandleQuoteDTO quote = CandleQuoteDTO.builder().c(new Double[]{1.0, 2.0, 3.0, 4.0})
                .h(new Double[]{5.0, 6.0, 7.0, 8.0})
                .l(new Double[]{9.0, 10.0, 11.0, 12.0})
                .o(new Double[]{13.0, 14.0, 15.0, 16.0})
                .t(new Long[]{17L, 18L, 19L, 20L})
                .v(new Long[]{21L, 22L, 23L, 24L}).build();

        when(this.stockService.getCandleQuoteByShortName("shortName", 1)).thenReturn(quote);

        Assertions.assertEquals(quote, stockController.getCandleQuoteByShortName("shortName", 1).getBody());
    }
}