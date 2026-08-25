package eye.on.the.money.service.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eye.on.the.money.exception.APIException;
import eye.on.the.money.model.Config;
import eye.on.the.money.repository.ConfigRepository;
import eye.on.the.money.repository.CredentialRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CryptoAPIServiceTest {

    private static final String API = "coingecko";

    private CredentialRepository credentialRepository;
    private ConfigRepository configRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        this.credentialRepository = mock(CredentialRepository.class);
        this.configRepository = mock(ConfigRepository.class);
        when(this.configRepository.findById(API)).thenReturn(Optional.of(new Config(API, "https://api.coingecko.com/api/v3")));
    }

    private CryptoAPIService serviceWithResponse(int statusCode, String body) {
        return this.serviceWithResponse(statusCode, body, new ArrayList<>());
    }

    private CryptoAPIService serviceWithResponse(int statusCode, String body, List<String> requestedUrls) {
        WebClient webClient = WebClient.builder()
                .exchangeFunction(request -> {
                    requestedUrls.add(request.url().toString());
                    return Mono.just(
                            ClientResponse.create(HttpStatus.valueOf(statusCode))
                                    .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                    .body(body)
                                    .build());
                })
                .build();
        return new CryptoAPIService(this.credentialRepository, this.configRepository, webClient, this.objectMapper);
    }

    @Test
    void getLiveValueForCoins_returnsParsedJson() {
        String responseBody = "{\"bitcoin\":{\"usd\":50000.0,\"usd_24h_change\":2.5}}";
        CryptoAPIService service = this.serviceWithResponse(200, responseBody);

        JsonNode result = service.getLiveValueForCoins("usd", "bitcoin");

        assertNotNull(result);
        assertEquals(50000.0, result.get("bitcoin").get("usd").asDouble());
        assertEquals(2.5, result.get("bitcoin").get("usd_24h_change").asDouble());
    }

    @Test
    void getLiveValueForCoins_handlesMultipleCoins() {
        String responseBody = "{\"bitcoin\":{\"eur\":45000.0},\"ethereum\":{\"eur\":3000.0}}";
        CryptoAPIService service = this.serviceWithResponse(200, responseBody);

        JsonNode result = service.getLiveValueForCoins("eur", "bitcoin,ethereum");

        assertNotNull(result);
        assertTrue(result.has("bitcoin"));
        assertTrue(result.has("ethereum"));
        assertEquals(3000.0, result.get("ethereum").get("eur").asDouble());
    }

    @Test
    void getLiveValueForCoins_buildsUrlWithoutToken() {
        List<String> requestedUrls = new ArrayList<>();
        CryptoAPIService service = this.serviceWithResponse(200, "{\"bitcoin\":{\"usd\":50000.0}}", requestedUrls);

        service.getLiveValueForCoins("usd", "bitcoin");

        assertEquals(1, requestedUrls.size());
        assertFalse(requestedUrls.getFirst().contains("token"));
        assertEquals("https://api.coingecko.com/api/v3/simple/price?ids=bitcoin&vs_currencies=usd&include_24hr_change=true",
                requestedUrls.getFirst());
        verifyNoInteractions(this.credentialRepository);
    }

    @Test
    void getLiveValueForCoins_throwsOnApiError() {
        CryptoAPIService service = this.serviceWithResponse(500, "Internal Server Error");

        assertThrows(APIException.class, () -> service.getLiveValueForCoins("usd", "bitcoin"));
    }

    @Test
    void getLiveValueForCoins_throwsOnNotFound() {
        CryptoAPIService service = this.serviceWithResponse(404, "Not Found");

        assertThrows(APIException.class, () -> service.getLiveValueForCoins("usd", "bitcoin"));
    }
}
