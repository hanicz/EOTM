package eye.on.the.money.service.api.impl;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eye.on.the.money.EotmApplication;
import eye.on.the.money.exception.APIException;
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
import java.util.ArrayList;
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

    @BeforeEach
    public void init() {
        this.mockServer = MockRestServiceServer.createServer(restTemplate);
    }

    @Test
    public void getLiveValue() throws URISyntaxException, JsonProcessingException {
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
    public void getAllSymbolsWrongJsonObject() throws URISyntaxException, JsonProcessingException {
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
    public void getAllExchangesWrongJsonObject() throws URISyntaxException, JsonProcessingException {
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
    public void clientException() throws URISyntaxException, JsonProcessingException {
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
}
