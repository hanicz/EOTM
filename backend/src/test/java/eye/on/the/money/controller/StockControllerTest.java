package eye.on.the.money.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import eye.on.the.money.EotmApplication;
import eye.on.the.money.model.stock.Exchange;
import eye.on.the.money.model.stock.Stock;
import eye.on.the.money.model.stock.Symbol;
import eye.on.the.money.repository.stock.StockRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = EotmApplication.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class StockControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private StockRepository stockRepository;
    private final ObjectMapper om = new ObjectMapper();
    private MockRestServiceServer mockServer;

    @BeforeEach
    public void init() {
        this.mockServer = MockRestServiceServer.createServer(this.restTemplate);
    }


    @Test
    public void getAllStocks() throws Exception {
        List<Stock> stocks = this.stockRepository.findAllByOrderByShortNameAsc();
        MvcResult response = this.mockMvc.perform(get("/stock")).andExpect(status().isOk()).andReturn();

        List<Stock> result = om.readValue(response.getResponse().getContentAsString(), new TypeReference<>() {
        });

        Assertions.assertIterableEquals(stocks, result);
    }

    @Test
    public void getAllSymbols() throws Exception {
        List<Symbol> symbols = new ArrayList<>();
        symbols.add(Symbol.builder().name("N1").code("C1").type("T1").build());
        symbols.add(Symbol.builder().name("N2").code("C2").type("T2").build());
        symbols.add(Symbol.builder().name("N3").code("C3").type("T3").build());

        this.mockServer.expect(ExpectedCount.once(),
                        requestTo(new URI("https://eodhost.com/exchange-symbol-list/US?api_token=token&fmt=json")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(this.om.writeValueAsString(symbols)));

        MvcResult response = this.mockMvc.perform(get("/stock/symbols/US")).andExpect(status().isOk()).andReturn();
        List<Symbol> result = om.readValue(response.getResponse().getContentAsString(), new TypeReference<>() {
        });

        this.mockServer.verify();
        Assertions.assertIterableEquals(symbols, result);
    }

    @Test
    public void getAllExchanges() throws Exception {
        List<Exchange> exchanges = new ArrayList<>();
        exchanges.add(Exchange.builder().name("N1").code("C1").currency("CU1").build());
        exchanges.add(Exchange.builder().name("N2").code("C2").currency("CU2").build());
        exchanges.add(Exchange.builder().name("N3").code("C3").currency("CU3").build());


        this.mockServer.expect(ExpectedCount.once(),
                        requestTo(new URI("https://eodhost.com/exchanges-list?api_token=token&fmt=json")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(this.om.writeValueAsString(exchanges)));

        MvcResult response = this.mockMvc.perform(get("/stock/exchanges")).andExpect(status().isOk()).andReturn();
        List<Exchange> result = om.readValue(response.getResponse().getContentAsString(), new TypeReference<>() {
        });

        this.mockServer.verify();
        Assertions.assertIterableEquals(exchanges, result);
    }
}