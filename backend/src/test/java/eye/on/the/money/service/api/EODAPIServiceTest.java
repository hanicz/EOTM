package eye.on.the.money.service.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import eye.on.the.money.dto.in.EODCandleQuoteDTO;
import eye.on.the.money.exception.APIException;
import eye.on.the.money.model.Config;
import eye.on.the.money.model.Credential;
import eye.on.the.money.model.stock.Exchange;
import eye.on.the.money.model.stock.Symbol;
import eye.on.the.money.repository.ConfigRepository;
import eye.on.the.money.repository.CredentialRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EODAPIServiceTest {

    private static final String API = "eod";

    private CredentialRepository credentialRepository;
    private ConfigRepository configRepository;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @BeforeEach
    void setUp() {
        this.credentialRepository = mock(CredentialRepository.class);
        this.configRepository = mock(ConfigRepository.class);
        when(this.configRepository.findById(API)).thenReturn(Optional.of(new Config(API, "https://eodhistoricaldata.com/api")));
        when(this.credentialRepository.findById(API)).thenReturn(Optional.of(new Credential(API, "testToken")));
    }

    private EODAPIService serviceWithResponse(int statusCode, String body) {
        WebClient webClient = WebClient.builder()
                .exchangeFunction(request -> Mono.just(
                        ClientResponse.create(HttpStatus.valueOf(statusCode))
                                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                .body(body)
                                .build()))
                .build();
        return new EODAPIService(this.credentialRepository, this.configRepository, webClient, this.objectMapper);
    }

    @Test
    void getLiveStockValue_returnsParsedJson() {
        String responseBody = "[{\"code\":\"AAPL.US\",\"close\":150.0},{\"code\":\"MSFT.US\",\"close\":300.0}]";
        EODAPIService service = this.serviceWithResponse(200, responseBody);

        JsonNode result = service.getLiveStockValue("AAPL.US,MSFT.US");

        assertNotNull(result);
        assertTrue(result.isArray());
        assertEquals(2, result.size());
    }

    @Test
    void getLiveStockValue_throwsOnApiError() {
        EODAPIService service = this.serviceWithResponse(500, "error");

        assertThrows(APIException.class, () -> service.getLiveStockValue("AAPL.US"));
    }

    @Test
    void getLiveEtfValue_returnsParsedJson() {
        String responseBody = "[{\"code\":\"SPY.US\",\"close\":450.0}]";
        EODAPIService service = this.serviceWithResponse(200, responseBody);

        JsonNode result = service.getLiveEtfValue("SPY.US");

        assertNotNull(result);
        assertTrue(result.isArray());
        assertEquals(1, result.size());
    }

    @Test
    void getLiveEtfValue_throwsOnApiError() {
        EODAPIService service = this.serviceWithResponse(500, "error");

        assertThrows(APIException.class, () -> service.getLiveEtfValue("SPY.US"));
    }

    @Test
    void getLiveForexValue_returnsParsedJson() {
        String responseBody = "[{\"code\":\"EURUSD.FOREX\",\"close\":1.08}]";
        EODAPIService service = this.serviceWithResponse(200, responseBody);

        JsonNode result = service.getLiveForexValue("EURUSD.FOREX");

        assertNotNull(result);
        assertTrue(result.isArray());
    }

    @Test
    void getLiveForexValue_throwsOnApiError() {
        EODAPIService service = this.serviceWithResponse(500, "error");

        assertThrows(APIException.class, () -> service.getLiveForexValue("EURUSD.FOREX"));
    }

    @Test
    void getLiveValueForSingle_returnsParsedJson() {
        String responseBody = "{\"code\":\"AAPL.US\",\"close\":150.0,\"previousClose\":149.0}";
        EODAPIService service = this.serviceWithResponse(200, responseBody);

        JsonNode result = service.getLiveValueForSingle("AAPL.US");

        assertNotNull(result);
        assertEquals("AAPL.US", result.get("code").asText());
        assertEquals(150.0, result.get("close").asDouble());
    }

    @Test
    void getLiveValueForSingle_throwsOnApiError() {
        EODAPIService service = this.serviceWithResponse(500, "error");

        assertThrows(APIException.class, () -> service.getLiveValueForSingle("AAPL.US"));
    }

    @Test
    void getCandleQuoteByShortName_returnsCandleData() {
        String responseBody = "[{\"date\":\"2025-01-01\",\"open\":140.0,\"high\":155.0,\"low\":138.0,\"close\":150.0,\"volume\":1000000}]";
        EODAPIService service = this.serviceWithResponse(200, responseBody);

        List<EODCandleQuoteDTO> result = service.getCandleQuoteByShortName("AAPL.US", 6);

        assertEquals(1, result.size());
        assertEquals(150.0, result.get(0).getClose());
        assertEquals(140.0, result.get(0).getOpen());
        assertEquals(155.0, result.get(0).getHigh());
        assertEquals(138.0, result.get(0).getLow());
        assertEquals(1000000L, result.get(0).getVolume());
    }

    @Test
    void getCandleQuoteByShortName_usesDailyPeriodForShortRange() {
        String responseBody = "[{\"date\":\"2025-01-01\",\"open\":140.0,\"high\":155.0,\"low\":138.0,\"close\":150.0,\"volume\":1000000}]";
        EODAPIService service = this.serviceWithResponse(200, responseBody);

        List<EODCandleQuoteDTO> result = service.getCandleQuoteByShortName("AAPL.US", 12);

        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    void getCandleQuoteByShortName_usesWeeklyPeriodForMediumRange() {
        String responseBody = "[{\"date\":\"2025-01-01\",\"open\":140.0,\"high\":155.0,\"low\":138.0,\"close\":150.0,\"volume\":1000000}]";
        EODAPIService service = this.serviceWithResponse(200, responseBody);

        List<EODCandleQuoteDTO> result = service.getCandleQuoteByShortName("AAPL.US", 36);

        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    void getCandleQuoteByShortName_usesMonthlyPeriodForLongRange() {
        String responseBody = "[{\"date\":\"2025-01-01\",\"open\":140.0,\"high\":155.0,\"low\":138.0,\"close\":150.0,\"volume\":1000000}]";
        EODAPIService service = this.serviceWithResponse(200, responseBody);

        List<EODCandleQuoteDTO> result = service.getCandleQuoteByShortName("AAPL.US", 120);

        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    void getCandleQuoteByShortName_throwsOnApiError() {
        EODAPIService service = this.serviceWithResponse(500, "error");

        assertThrows(APIException.class, () -> service.getCandleQuoteByShortName("AAPL.US", 6));
    }

    @Test
    void getAllSymbols_returnsSymbolList() {
        String responseBody = "[{\"Code\":\"AAPL\",\"Name\":\"Apple Inc\",\"Type\":\"Common Stock\"},{\"Code\":\"MSFT\",\"Name\":\"Microsoft\",\"Type\":\"Common Stock\"}]";
        EODAPIService service = this.serviceWithResponse(200, responseBody);

        List<Symbol> result = service.getAllSymbols("US");

        assertEquals(2, result.size());
        assertEquals("AAPL", result.get(0).getCode());
        assertEquals("Apple Inc", result.get(0).getName());
        assertEquals("Common Stock", result.get(0).getType());
    }

    @Test
    void getAllSymbols_throwsOnApiError() {
        EODAPIService service = this.serviceWithResponse(500, "error");

        assertThrows(APIException.class, () -> service.getAllSymbols("US"));
    }

    @Test
    void getAllExchanges_returnsExchangeList() {
        String responseBody = "[{\"Code\":\"US\",\"Name\":\"US Exchanges\",\"Currency\":\"USD\"},{\"Code\":\"LSE\",\"Name\":\"London Stock Exchange\",\"Currency\":\"GBP\"}]";
        EODAPIService service = this.serviceWithResponse(200, responseBody);

        List<Exchange> result = service.getAllExchanges();

        assertEquals(2, result.size());
        assertEquals("US", result.get(0).getCode());
        assertEquals("US Exchanges", result.get(0).getName());
        assertEquals("USD", result.get(0).getCurrency());
    }

    @Test
    void getAllExchanges_throwsOnApiError() {
        EODAPIService service = this.serviceWithResponse(500, "error");

        assertThrows(APIException.class, () -> service.getAllExchanges());
    }
}
