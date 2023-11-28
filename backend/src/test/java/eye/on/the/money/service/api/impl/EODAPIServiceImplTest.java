package eye.on.the.money.service.api.impl;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eye.on.the.money.EotmApplication;
import eye.on.the.money.exception.APIException;
import eye.on.the.money.model.stock.EODCandleQuote;
import eye.on.the.money.model.stock.Exchange;
import eye.on.the.money.model.stock.Symbol;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

@SpringBootTest(classes = EotmApplication.class)
@ActiveProfiles("test")
class EODAPIServiceImplTest {

    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private EODAPIServiceImpl eodAPIService;
    private MockRestServiceServer mockServer;
    private final ObjectMapper mapper = new ObjectMapper();
    private final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    @BeforeEach
    public void init() {
        this.mockServer = MockRestServiceServer.createServer(this.restTemplate);
    }

    @Test
    public void getLiveValue() throws URISyntaxException {
        String json = "{\"json\":\"json\"}";
        this.mockServer.expect(ExpectedCount.once(),
                        requestTo(new URI("https://eodhost.com/real-time/stock/?api_token=token&fmt=json&s=AMD.US")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(json));

        JsonNode response = this.eodAPIService.getLiveValue("AMD.US", "/real-time/stock/?api_token={0}&fmt=json&s={1}");
        this.mockServer.verify();
        Assertions.assertEquals(json, response.toString());
    }

    @Test
    public void getLiveValueForSingle() throws URISyntaxException {
        String json = "{\"json\":\"json\"}";
        this.mockServer.expect(ExpectedCount.once(),
                        requestTo(new URI("https://eodhost.com/real-time/AMD.US/?api_token=token&fmt=json")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(json));

        JsonNode response = this.eodAPIService.getLiveValue("AMD.US", "/real-time/{1}/?api_token={0}&fmt=json");
        this.mockServer.verify();
        Assertions.assertEquals(json, response.toString());
    }

    @Test
    public void getAllSymbols() throws URISyntaxException, JsonProcessingException {
        List<Symbol> symbols = new ArrayList<>();
        symbols.add(Symbol.builder().name("N1").code("C1").type("T1").build());
        symbols.add(Symbol.builder().name("N2").code("C2").type("T2").build());
        symbols.add(Symbol.builder().name("N3").code("C3").type("T3").build());

        this.mockServer.expect(ExpectedCount.once(),
                        requestTo(new URI("https://eodhost.com/exchange-symbol-list/US?api_token=token&fmt=json")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(this.mapper.writeValueAsString(symbols)));

        List<Symbol> response = this.eodAPIService.getAllSymbols("US");
        this.mockServer.verify();
        Assertions.assertEquals(symbols, response);
    }

    @Test
    public void getAllSymbolsWrongJsonObject() throws URISyntaxException {
        String json = "{\"json\":\"json\"}";

        this.mockServer.expect(ExpectedCount.times(3),
                        requestTo(new URI("https://eodhost.com/exchange-symbol-list/US?api_token=token&fmt=json")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(json));

        Exception e = Assertions.assertThrows(APIException.class, () -> {
            this.eodAPIService.getAllSymbols("US");
        });

        String expectedMessage = "Symbol JSON process failed";
        String actualMessage = e.getMessage();

        Assertions.assertEquals(expectedMessage, actualMessage);
        this.mockServer.verify();
    }

    @Test
    public void getAllExchanges() throws URISyntaxException, JsonProcessingException {
        List<Exchange> exchanges = new ArrayList<>();
        exchanges.add(Exchange.builder().name("N1").code("C1").currency("CU1").build());
        exchanges.add(Exchange.builder().name("N2").code("C2").currency("CU2").build());
        exchanges.add(Exchange.builder().name("N3").code("C3").currency("CU3").build());


        this.mockServer.expect(ExpectedCount.once(),
                        requestTo(new URI("https://eodhost.com/exchanges-list?api_token=token&fmt=json")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(this.mapper.writeValueAsString(exchanges)));

        List<Exchange> response = this.eodAPIService.getAllExchanges();
        this.mockServer.verify();
        Assertions.assertEquals(exchanges, response);
    }

    @Test
    public void getAllExchangesWrongJsonObject() throws URISyntaxException {
        String json = "{\"json\":\"json\"}";

        this.mockServer.expect(ExpectedCount.times(3),
                        requestTo(new URI("https://eodhost.com/exchanges-list?api_token=token&fmt=json")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(json));

        Exception e = Assertions.assertThrows(APIException.class, () -> {
            this.eodAPIService.getAllExchanges();
        });

        String expectedMessage = "Exchange JSON process failed";
        String actualMessage = e.getMessage();

        Assertions.assertEquals(expectedMessage, actualMessage);
        this.mockServer.verify();
    }

    @Test
    public void invalidJson() throws URISyntaxException {
        String json = "not JSON";
        this.mockServer.expect(ExpectedCount.times(3),
                        requestTo(new URI("https://eodhost.com/real-time/stock/?api_token=token&fmt=json&s=AMD.US")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(json));

        Exception e = Assertions.assertThrows(APIException.class, () -> {
            this.eodAPIService.getLiveValue("AMD.US", "/real-time/stock/?api_token={0}&fmt=json&s={1}");
        });

        this.mockServer.verify();

        String expectedMessage = "JSON process failed";
        String actualMessage = e.getMessage();

        Assertions.assertEquals(expectedMessage, actualMessage);
    }

    @Test
    public void clientException() throws URISyntaxException {
        this.mockServer.expect(ExpectedCount.times(3),
                        requestTo(new URI("https://eodhost.com/real-time/stock/?api_token=token&fmt=json&s=AMD.US")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withBadRequest());

        Exception e = Assertions.assertThrows(APIException.class, () -> {
            this.eodAPIService.getLiveValue("AMD.US", "/real-time/stock/?api_token={0}&fmt=json&s={1}");
        });

        this.mockServer.verify();

        String expectedMessage = "Unable to reach stock API.";
        String actualMessage = e.getMessage();

        Assertions.assertEquals(expectedMessage, actualMessage);
    }

    @Test
    public void getCandleQuoteByShortName5Years() throws URISyntaxException, JsonProcessingException {
        List<EODCandleQuote> candles = this.getCandles();

        this.mockServer.expect(ExpectedCount.once(),
                        requestTo(new URI("https://eodhost.com/eod/AMD.US?api_token=token&fmt=json&period=m")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(this.mapper.writeValueAsString(candles)));

        List<EODCandleQuote> result = this.eodAPIService.getCandleQuoteByShortName("AMD.US", 61);
        this.mockServer.verify();
        Assertions.assertEquals(candles, result);
    }

    @Test
    public void getCandleQuoteByShortName2Years() throws URISyntaxException, JsonProcessingException {
        List<EODCandleQuote> candles = this.getCandles();

        String from = this.dateFormat.format(Date.from(ZonedDateTime.now().minusMonths(24).toInstant()));

        this.mockServer.expect(ExpectedCount.once(),
                        requestTo(new URI("https://eodhost.com/eod/AMD.US?api_token=token&fmt=json&period=w&from=" + from)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(this.mapper.writeValueAsString(candles)));

        List<EODCandleQuote> result = this.eodAPIService.getCandleQuoteByShortName("AMD.US", 24);
        this.mockServer.verify();
        Assertions.assertEquals(candles, result);
    }

    @Test
    public void getCandleQuoteByShortName1Year() throws URISyntaxException, JsonProcessingException {
        List<EODCandleQuote> candles = this.getCandles();

        String from = this.dateFormat.format(Date.from(ZonedDateTime.now().minusMonths(12).toInstant()));

        this.mockServer.expect(ExpectedCount.once(),
                        requestTo(new URI("https://eodhost.com/eod/AMD.US?api_token=token&fmt=json&period=d&from=" + from)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(this.mapper.writeValueAsString(candles)));

        List<EODCandleQuote> result = this.eodAPIService.getCandleQuoteByShortName("AMD.US", 12);
        this.mockServer.verify();
        Assertions.assertEquals(candles, result);
    }

    @Test
    public void getCandleQuoteByShortName6Months() throws URISyntaxException, JsonProcessingException {
        List<EODCandleQuote> candles = this.getCandles();

        String from = this.dateFormat.format(Date.from(ZonedDateTime.now().minusMonths(6).toInstant()));

        this.mockServer.expect(ExpectedCount.once(),
                        requestTo(new URI("https://eodhost.com/eod/AMD.US?api_token=token&fmt=json&period=d&from=" + from)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(this.mapper.writeValueAsString(candles)));

        List<EODCandleQuote> result = this.eodAPIService.getCandleQuoteByShortName("AMD.US", 6);
        this.mockServer.verify();
        Assertions.assertEquals(candles, result);
    }

    @Test
    public void getCandleQuoteByShortName1Month() throws URISyntaxException, JsonProcessingException {
        List<EODCandleQuote> candles = this.getCandles();

        String from = this.dateFormat.format(Date.from(ZonedDateTime.now().minusMonths(1).toInstant()));

        this.mockServer.expect(ExpectedCount.once(),
                        requestTo(new URI("https://eodhost.com/eod/AMD.US?api_token=token&fmt=json&period=d&from=" + from)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(this.mapper.writeValueAsString(candles)));

        List<EODCandleQuote> result = this.eodAPIService.getCandleQuoteByShortName("AMD.US", 1);
        this.mockServer.verify();
        Assertions.assertEquals(candles, result);
    }

    private List<EODCandleQuote> getCandles() {
        List<EODCandleQuote> candles = new ArrayList<>();
        candles.add(EODCandleQuote.builder().volume(1L).open(1.0).low(2.0).high(5.0).date(new Date()).close(55.5).build());
        candles.add(EODCandleQuote.builder().volume(62134L).open(1.7).low(2.0).high(4.9).date(new Date()).close(64.5).build());
        candles.add(EODCandleQuote.builder().volume(1231L).open(1.22).low(25.1).high(95.3).date(new Date()).close(51.5).build());

        return candles;
    }
}
