package eye.on.the.money.service.stock.impl;

import eye.on.the.money.EotmApplication;
import eye.on.the.money.model.stock.Exchange;
import eye.on.the.money.model.stock.Stock;
import eye.on.the.money.model.stock.Symbol;
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
import java.util.List;

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

        Assertions.assertEquals(stocks, result);
    }

    @Test
    public void getAllSymbols() {
        List<Symbol> symbols = new ArrayList<>();
        symbols.add(Symbol.builder().name("N1").code("C1").type("T1").build());
        symbols.add(Symbol.builder().name("N2").code("C2").type("T2").build());
        symbols.add(Symbol.builder().name("N3").code("C3").type("T3").build());

        when(this.eodAPIService.getAllSymbols("exchange")).thenReturn(symbols);

        List<Symbol> result = this.stockService.getAllSymbols("exchange");

        Assertions.assertEquals(symbols, result);
    }

    @Test
    public void getAllExchanges() {
        List<Exchange> exchanges = new ArrayList<>();
        exchanges.add(Exchange.builder().name("N1").code("C1").currency("CU1").build());
        exchanges.add(Exchange.builder().name("N2").code("C2").currency("CU2").build());
        exchanges.add(Exchange.builder().name("N3").code("C3").currency("CU3").build());

        when(this.eodAPIService.getAllExchanges()).thenReturn(exchanges);

        List<Exchange> result = this.stockService.getAllExchanges();

        Assertions.assertEquals(exchanges, result);
    }

    @Test
    public void getCandleQuoteByShortName() {

    }
}