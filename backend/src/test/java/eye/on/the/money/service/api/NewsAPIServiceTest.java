package eye.on.the.money.service.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import eye.on.the.money.exception.APIException;
import eye.on.the.money.model.Config;
import eye.on.the.money.model.Credential;
import eye.on.the.money.model.news.News;
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

class NewsAPIServiceTest {

    private static final String API = "finnhub";

    private CredentialRepository credentialRepository;
    private ConfigRepository configRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        this.credentialRepository = mock(CredentialRepository.class);
        this.configRepository = mock(ConfigRepository.class);
        when(this.configRepository.findById(API)).thenReturn(Optional.of(new Config(API, "https://finnhub.io/api/v1")));
        when(this.credentialRepository.findById(API)).thenReturn(Optional.of(new Credential(API, "testToken")));
    }

    private NewsAPIService serviceWithResponse(int statusCode, String body) {
        WebClient webClient = WebClient.builder()
                .exchangeFunction(request -> Mono.just(
                        ClientResponse.create(HttpStatus.valueOf(statusCode))
                                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                .body(body)
                                .build()))
                .build();
        return new NewsAPIService(this.credentialRepository, this.configRepository, webClient, this.objectMapper);
    }

    @Test
    void getNews_returnsNewsList() {
        String responseBody = "[{\"id\":1,\"category\":\"general\",\"headline\":\"Market Update\",\"source\":\"Reuters\",\"summary\":\"Markets are up\",\"url\":\"https://example.com/1\"}," +
                "{\"id\":2,\"category\":\"general\",\"headline\":\"Tech Rally\",\"source\":\"Bloomberg\",\"summary\":\"Tech stocks rally\",\"url\":\"https://example.com/2\"}]";
        NewsAPIService service = this.serviceWithResponse(200, responseBody);

        List<News> result = service.getNews("general");

        assertEquals(2, result.size());
        assertEquals("Market Update", result.get(0).getHeadline());
        assertEquals("Reuters", result.get(0).getSource());
        assertEquals("Tech Rally", result.get(1).getHeadline());
    }

    @Test
    void getNews_returnsEmptyListWhenNoNews() {
        NewsAPIService service = this.serviceWithResponse(200, "[]");

        List<News> result = service.getNews("general");

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getNews_throwsOnApiError() {
        NewsAPIService service = this.serviceWithResponse(500, "error");

        assertThrows(APIException.class, () -> service.getNews("general"));
    }

    @Test
    void getNews_throwsOnNotFound() {
        NewsAPIService service = this.serviceWithResponse(404, "not found");

        assertThrows(APIException.class, () -> service.getNews("general"));
    }

    @Test
    void getCompanyNews_returnsNewsList() {
        String responseBody = "[{\"id\":10,\"category\":\"company\",\"headline\":\"Apple Earnings\",\"source\":\"CNBC\",\"summary\":\"Apple beats estimates\",\"url\":\"https://example.com/10\"}]";
        NewsAPIService service = this.serviceWithResponse(200, responseBody);

        List<News> result = service.getCompanyNews("AAPL");

        assertEquals(1, result.size());
        assertEquals("Apple Earnings", result.get(0).getHeadline());
        assertEquals("CNBC", result.get(0).getSource());
    }

    @Test
    void getCompanyNews_returnsEmptyListWhenNoNews() {
        NewsAPIService service = this.serviceWithResponse(200, "[]");

        List<News> result = service.getCompanyNews("AAPL");

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getCompanyNews_throwsOnApiError() {
        NewsAPIService service = this.serviceWithResponse(500, "error");

        assertThrows(APIException.class, () -> service.getCompanyNews("AAPL"));
    }
}
