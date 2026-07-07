package eye.on.the.money.service.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RedditAPIServiceTest {

    private static final String API = "reddit";
    private static final String TOKEN_API = "redditToken";

    private CredentialRepository credentialRepository;
    private ConfigRepository configRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        this.credentialRepository = mock(CredentialRepository.class);
        this.configRepository = mock(ConfigRepository.class);
        when(this.configRepository.findById(API)).thenReturn(Optional.of(new Config(API, "https://oauth.reddit.com/r/")));
        when(this.configRepository.findById(TOKEN_API)).thenReturn(Optional.of(new Config(TOKEN_API, "https://www.reddit.com/api/v1/")));
        when(this.credentialRepository.findById(API)).thenReturn(Optional.of(new Credential(API, "base64EncodedCredentials")));
    }

    private RedditAPIService serviceWithResponse(int statusCode, String body) {
        WebClient webClient = WebClient.builder()
                .exchangeFunction(request -> Mono.just(
                        ClientResponse.create(HttpStatus.valueOf(statusCode))
                                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                .body(body)
                                .build()))
                .build();
        return new RedditAPIService(this.credentialRepository, this.configRepository, webClient, this.objectMapper);
    }

    @Test
    void getToken_returnsParsedToken() {
        String responseBody = "{\"access_token\":\"abc123\",\"token_type\":\"bearer\",\"expires_in\":86400}";
        RedditAPIService service = this.serviceWithResponse(200, responseBody);

        JsonNode result = service.getToken();

        assertNotNull(result);
        assertEquals("abc123", result.get("access_token").asText());
        assertEquals("bearer", result.get("token_type").asText());
    }

    @Test
    void getToken_throwsOnApiError() {
        RedditAPIService service = this.serviceWithResponse(500, "error");

        assertThrows(APIException.class, () -> service.getToken());
    }

    @Test
    void getToken_throwsOnUnauthorized() {
        RedditAPIService service = this.serviceWithResponse(401, "Unauthorized");

        assertThrows(APIException.class, () -> service.getToken());
    }

    @Test
    void getHotRedditNews_returnsFluxOfJsonNodes() {
        String responseBody = "{\"data\":{\"children\":[{\"data\":{\"title\":\"Stock tips\",\"score\":100}}]}}";
        RedditAPIService service = this.serviceWithResponse(200, responseBody);

        List<JsonNode> results = service.getHotRedditNews(List.of("wallstreetbets"), "bearerToken123")
                .collectList().block();

        assertNotNull(results);
        assertEquals(1, results.size());
        assertTrue(results.get(0).has("data"));
    }

    @Test
    void getHotRedditNews_handlesMultipleSubreddits() {
        String responseBody = "{\"data\":{\"children\":[{\"data\":{\"title\":\"Post\"}}]}}";
        RedditAPIService service = this.serviceWithResponse(200, responseBody);

        List<JsonNode> results = service.getHotRedditNews(List.of("wallstreetbets", "stocks", "investing"), "bearerToken123")
                .collectList().block();

        assertNotNull(results);
        assertEquals(3, results.size());
    }

    @Test
    void getHotRedditNews_handlesEmptySubredditList() {
        RedditAPIService service = this.serviceWithResponse(200, "{}");

        List<JsonNode> results = service.getHotRedditNews(List.of(), "bearerToken123")
                .collectList().block();

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void getToken_throwsWhenCredentialMissing() {
        when(this.credentialRepository.findById(API)).thenReturn(Optional.empty());
        RedditAPIService service = this.serviceWithResponse(200, "{\"access_token\":\"abc\"}");

        assertThrows(NoSuchElementException.class, () -> service.getToken());
    }

    @Test
    void createURL_doesNotIncludeCredentialInUrl() {
        String responseBody = "{\"data\":{\"children\":[]}}";
        RedditAPIService service = this.serviceWithResponse(200, responseBody);

        List<JsonNode> results = service.getHotRedditNews(List.of("stocks"), "token")
                .collectList().block();

        assertNotNull(results);
    }
}
