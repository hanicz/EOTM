package eye.on.the.money.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import eye.on.the.money.EotmApplication;
import eye.on.the.money.model.stock.*;
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
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = EotmApplication.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class StockControllerIntegrationTest {

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

    @Test
    public void getCandleQuoteByShortNameSameDay() throws Exception {
        List<EODCandleQuote> eodList = this.geteodList();

        this.mockServer.expect(ExpectedCount.once(),
                        requestTo(new URI("https://eodhost.com/eod/AMD.US?api_token=token&fmt=json&period=m")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(this.om.writeValueAsString(eodList)));

        this.mockServer.expect(ExpectedCount.once(),
                        requestTo(new URI("https://eodhost.com/real-time/AMD.US/?api_token=token&fmt=json")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"code\":\"AMD.US\",\"timestamp\":" + TimeUnit.MILLISECONDS.toSeconds(new Date().getTime()) + ",\"gmtoffset\":0,\"open\":119.64,\"high\":119.97,\"low\":118.82,\"close\":119.7044,\"volume\":5094170,\"previousClose\":119.83,\"change\":-0.1256,\"change_p\":-0.1048}"));

        MvcResult response = this.mockMvc.perform(get("/stock/candle/AMD.US/100")).andExpect(status().isOk()).andReturn();
        CandleQuote result = om.readValue(response.getResponse().getContentAsString(), CandleQuote.class);

        Assertions.assertAll("Assert all cq arrays",
                () -> assertArrayEquals(eodList.stream().map(EODCandleQuote::getHigh).toArray(Double[]::new), result.getH()),
                () -> assertArrayEquals(eodList.stream().map(EODCandleQuote::getLow).toArray(Double[]::new), result.getL()),
                () -> assertArrayEquals(eodList.stream().map(EODCandleQuote::getClose).toArray(Double[]::new), result.getC()),
                () -> assertArrayEquals(eodList.stream().map(EODCandleQuote::getVolume).toArray(Long[]::new), result.getV()),
                () -> assertArrayEquals(eodList.stream().map(EODCandleQuote::getOpen).toArray(Double[]::new), result.getO()),
                () -> assertArrayEquals(eodList.stream().map(ecq -> ecq.getDate().getTime()).toArray(Long[]::new), result.getT()));
    }

    @Test
    public void getCandleQuoteByShortNameNotSameDay() throws Exception {
        List<EODCandleQuote> eodList = this.geteodList();

        this.mockServer.expect(ExpectedCount.once(),
                        requestTo(new URI("https://eodhost.com/eod/AMD.US?api_token=token&fmt=json&period=m")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(this.om.writeValueAsString(eodList)));

        this.mockServer.expect(ExpectedCount.once(),
                        requestTo(new URI("https://eodhost.com/real-time/AMD.US/?api_token=token&fmt=json")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"code\":\"AMD.US\",\"timestamp\": 1700255640,\"gmtoffset\":0,\"open\":119.64,\"high\":119.97,\"low\":118.82,\"close\":119.7044,\"volume\":5094170,\"previousClose\":119.83,\"change\":-0.1256,\"change_p\":-0.1048}"));

        MvcResult response = this.mockMvc.perform(get("/stock/candle/AMD.US/100")).andExpect(status().isOk()).andReturn();
        CandleQuote result = om.readValue(response.getResponse().getContentAsString(), CandleQuote.class);

        eodList.add(EODCandleQuote.builder().close(119.7044).date(new Date(TimeUnit.SECONDS.toMillis(1700255640))).high(119.97).low(118.82).open(119.64).volume(5094170L).build());

        Assertions.assertAll("Assert all cq arrays",
                () -> assertArrayEquals(eodList.stream().map(EODCandleQuote::getHigh).toArray(Double[]::new), result.getH()),
                () -> assertArrayEquals(eodList.stream().map(EODCandleQuote::getLow).toArray(Double[]::new), result.getL()),
                () -> assertArrayEquals(eodList.stream().map(EODCandleQuote::getClose).toArray(Double[]::new), result.getC()),
                () -> assertArrayEquals(eodList.stream().map(EODCandleQuote::getVolume).toArray(Long[]::new), result.getV()),
                () -> assertArrayEquals(eodList.stream().map(EODCandleQuote::getOpen).toArray(Double[]::new), result.getO()),
                () -> assertArrayEquals(eodList.stream().map(ecq -> ecq.getDate().getTime()).toArray(Long[]::new), result.getT()));
    }

    private List<EODCandleQuote> geteodList() {
        List<EODCandleQuote> eodList = new ArrayList<>();
        eodList.add(EODCandleQuote.builder().close(1.0).date(new Date()).high(5.0).low(0.2).open(3.5).volume(5123123L).build());
        eodList.add(EODCandleQuote.builder().close(2.0).date(new Date()).high(532.0).low(0.9).open(323.5).volume(5123L).build());
        eodList.add(EODCandleQuote.builder().close(3.0).date(new Date()).high(51.0).low(301.4).open(13.8).volume(7234L).build());
        eodList.add(EODCandleQuote.builder().close(4.0).date(new Date()).high(55.0).low(200.0).open(553.5).volume(94123L).build());
        eodList.add(EODCandleQuote.builder().close(5.0).date(new Date()).high(25.0).low(100.0).open(37.5).volume(7213L).build());

        return eodList;
    }
}