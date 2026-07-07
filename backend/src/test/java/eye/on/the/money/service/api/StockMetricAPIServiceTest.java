package eye.on.the.money.service.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import eye.on.the.money.dto.out.MetricDTO;
import eye.on.the.money.dto.out.ProfileDTO;
import eye.on.the.money.dto.out.RecommendationDTO;
import eye.on.the.money.exception.APIException;
import eye.on.the.money.model.Config;
import eye.on.the.money.model.Credential;
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

class StockMetricAPIServiceTest {

    private static final String API = "finnhub";

    private CredentialRepository credentialRepository;
    private ConfigRepository configRepository;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @BeforeEach
    void setUp() {
        this.credentialRepository = mock(CredentialRepository.class);
        this.configRepository = mock(ConfigRepository.class);
        when(this.configRepository.findById(API)).thenReturn(Optional.of(new Config(API, "https://finnhub.io/api/v1")));
        when(this.credentialRepository.findById(API)).thenReturn(Optional.of(new Credential(API, "testToken")));
    }

    private StockMetricAPIService serviceWithResponse(int statusCode, String body) {
        WebClient webClient = WebClient.builder()
                .exchangeFunction(request -> Mono.just(
                        ClientResponse.create(HttpStatus.valueOf(statusCode))
                                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                .body(body)
                                .build()))
                .build();
        return new StockMetricAPIService(this.credentialRepository, this.configRepository, webClient, this.objectMapper);
    }

    @Test
    void getProfile_returnsProfileDTO() {
        String responseBody = "{\"country\":\"US\",\"currency\":\"USD\",\"exchange\":\"NASDAQ\"," +
                "\"finnhubIndustry\":\"Technology\",\"ipo\":\"1980-12-12\",\"logo\":\"https://logo.com/aapl.png\"," +
                "\"name\":\"Apple Inc\",\"ticker\":\"AAPL\",\"weburl\":\"https://apple.com\"," +
                "\"marketCapitalization\":2800000.0,\"shareOutstanding\":15000000}";
        StockMetricAPIService service = this.serviceWithResponse(200, responseBody);

        ProfileDTO result = service.getProfile("AAPL");

        assertNotNull(result);
        assertEquals("US", result.getCountry());
        assertEquals("USD", result.getCurrency());
        assertEquals("Apple Inc", result.getName());
        assertEquals("AAPL", result.getTicker());
        assertEquals("Technology", result.getFinnhubIndustry());
        assertEquals(2800000.0, result.getMarketCapitalization());
    }

    @Test
    void getProfile_throwsOnApiError() {
        StockMetricAPIService service = this.serviceWithResponse(500, "error");

        assertThrows(APIException.class, () -> service.getProfile("AAPL"));
    }

    @Test
    void getPeers_returnsPeerSymbols() {
        String responseBody = "[\"MSFT\",\"GOOGL\",\"META\",\"AMZN\"]";
        StockMetricAPIService service = this.serviceWithResponse(200, responseBody);

        String[] result = service.getPeers("AAPL");

        assertNotNull(result);
        assertEquals(4, result.length);
        assertEquals("MSFT", result[0]);
        assertEquals("GOOGL", result[1]);
    }

    @Test
    void getPeers_returnsEmptyArrayWhenNoPeers() {
        StockMetricAPIService service = this.serviceWithResponse(200, "[]");

        String[] result = service.getPeers("UNKNOWN");

        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    void getPeers_throwsOnApiError() {
        StockMetricAPIService service = this.serviceWithResponse(500, "error");

        assertThrows(APIException.class, () -> service.getPeers("AAPL"));
    }

    @Test
    void getMetric_returnsMetricDTO() {
        String responseBody = "{\"metric\":{\"10DayAverageTradingVolume\":50.5,\"3MonthAverageTradingVolume\":48.2," +
                "\"52WeekHigh\":199.62,\"52WeekHighDate\":\"2025-06-15\",\"52WeekLow\":130.67," +
                "\"52WeekLowDate\":\"2024-10-01\",\"peInclExtraTTM\":28.5}}";
        StockMetricAPIService service = this.serviceWithResponse(200, responseBody);

        MetricDTO result = service.getMetric("AAPL");

        assertNotNull(result);
        assertEquals(50.5, result.getTenDayAverageTradingVolume());
        assertEquals(48.2, result.getThreeMonthAverageTradingVolume());
        assertEquals(199.62, result.getYearHigh());
        assertEquals(130.67, result.getYearLow());
        assertEquals(28.5, result.getPeInclExtraTTM());
    }

    @Test
    void getMetric_throwsOnApiError() {
        StockMetricAPIService service = this.serviceWithResponse(500, "error");

        assertThrows(APIException.class, () -> service.getMetric("AAPL"));
    }

    @Test
    void getMetric_throwsOnInvalidJson() {
        StockMetricAPIService service = this.serviceWithResponse(200, "not json");

        assertThrows(APIException.class, () -> service.getMetric("AAPL"));
    }

    @Test
    void getMetric_returnsNullWhenMetricFieldMissing() {
        StockMetricAPIService service = this.serviceWithResponse(200, "{\"other\":\"data\"}");

        MetricDTO result = service.getMetric("AAPL");

        assertNull(result);
    }

    @Test
    void getRecommendations_returnsRecommendationList() {
        String responseBody = "[{\"buy\":10,\"hold\":5,\"period\":\"2025-06-01\",\"sell\":2,\"strongBuy\":8,\"strongSell\":1,\"symbol\":\"AAPL\"}," +
                "{\"buy\":12,\"hold\":4,\"period\":\"2025-05-01\",\"sell\":3,\"strongBuy\":6,\"strongSell\":0,\"symbol\":\"AAPL\"}]";
        StockMetricAPIService service = this.serviceWithResponse(200, responseBody);

        List<RecommendationDTO> result = service.getRecommendations("AAPL");

        assertEquals(2, result.size());
        assertEquals(10, result.get(0).getBuy());
        assertEquals(5, result.get(0).getHold());
        assertEquals(2, result.get(0).getSell());
        assertEquals(8, result.get(0).getStrongBuy());
        assertEquals(1, result.get(0).getStrongSell());
        assertEquals("AAPL", result.get(0).getSymbol());
    }

    @Test
    void getRecommendations_returnsEmptyListWhenNoData() {
        StockMetricAPIService service = this.serviceWithResponse(200, "[]");

        List<RecommendationDTO> result = service.getRecommendations("UNKNOWN");

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getRecommendations_throwsOnApiError() {
        StockMetricAPIService service = this.serviceWithResponse(500, "error");

        assertThrows(APIException.class, () -> service.getRecommendations("AAPL"));
    }
}
