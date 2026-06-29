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
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class APIServiceTest {

    private static final String API = "eod";

    private CredentialRepository credentialRepository;
    private ConfigRepository configRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        this.credentialRepository = mock(CredentialRepository.class);
        this.configRepository = mock(ConfigRepository.class);
    }

    private TestAPIService serviceWithResponse(int statusCode, String body) {
        WebClient webClient = WebClient.builder()
                .exchangeFunction(request -> Mono.just(
                        ClientResponse.create(HttpStatus.valueOf(statusCode))
                                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                .body(body)
                                .build()))
                .build();
        return new TestAPIService(this.credentialRepository, this.configRepository, webClient, this.objectMapper);
    }

    @Test
    void createURL_buildsUrlFromConfigSecretAndParams() {
        when(this.configRepository.findById(API)).thenReturn(Optional.of(new Config(API, "https://api.example.com")));
        when(this.credentialRepository.findById(API)).thenReturn(Optional.of(new Credential(API, "secretToken")));

        TestAPIService service = this.serviceWithResponse(200, "{}");
        String url = service.createURL(API, "/real-time/{1}/?api_token={0}&fmt=json", "AAPL.US");

        assertEquals("https://api.example.com/real-time/AAPL.US/?api_token=secretToken&fmt=json", url);
    }

    @Test
    void createURL_supportsMultipleParams() {
        when(this.configRepository.findById(API)).thenReturn(Optional.of(new Config(API, "https://api.example.com")));
        when(this.credentialRepository.findById(API)).thenReturn(Optional.of(new Credential(API, "secretToken")));

        TestAPIService service = this.serviceWithResponse(200, "{}");
        String url = service.createURL(API, "/real-time/{1}/?api_token={0}&fmt=json&s={2}", "AAPL.US", "MSFT.US");

        assertEquals("https://api.example.com/real-time/AAPL.US/?api_token=secretToken&fmt=json&s=MSFT.US", url);
    }

    @Test
    void createURL_throwsWhenConfigMissing() {
        when(this.configRepository.findById(API)).thenReturn(Optional.empty());

        TestAPIService service = this.serviceWithResponse(200, "{}");

        assertThrows(NoSuchElementException.class, () -> service.createURL(API, "/path"));
    }

    @Test
    void createURL_throwsWhenCredentialMissing() {
        when(this.configRepository.findById(API)).thenReturn(Optional.of(new Config(API, "https://api.example.com")));
        when(this.credentialRepository.findById(API)).thenReturn(Optional.empty());

        TestAPIService service = this.serviceWithResponse(200, "{}");

        assertThrows(NoSuchElementException.class, () -> service.createURL(API, "/path"));
    }

    @Test
    void callGetAPI_returnsBodyOnSuccess() {
        TestAPIService service = this.serviceWithResponse(200, "{\"price\":1.23}");

        ResponseEntity<String> response = service.callGetAPI("https://api.example.com/test", String.class);

        assertEquals("{\"price\":1.23}", response.getBody());
    }

    @Test
    void callGetAPI_throwsAPIExceptionOnErrorStatus() {
        TestAPIService service = this.serviceWithResponse(500, "error");

        assertThrows(APIException.class, () -> service.callGetAPI("https://api.example.com/test", String.class));
    }

    @Test
    void callGetAPI_throwsAPIExceptionOnNotFound() {
        TestAPIService service = this.serviceWithResponse(404, "not found");

        assertThrows(APIException.class, () -> service.callGetAPI("https://api.example.com/test", String.class));
    }

    @Test
    void callPostAPI_returnsBodyOnSuccess() {
        TestAPIService service = this.serviceWithResponse(200, "{\"status\":\"ok\"}");

        ResponseEntity<String> response = service.callPostAPI("https://api.example.com/test", String.class,
                headers -> headers.add("Content-Type", "application/json"), "{\"input\":1}");

        assertEquals("{\"status\":\"ok\"}", response.getBody());
    }

    @Test
    void callPostAPI_throwsAPIExceptionOnErrorStatus() {
        TestAPIService service = this.serviceWithResponse(500, "error");

        assertThrows(APIException.class, () -> service.callPostAPI("https://api.example.com/test", String.class,
                headers -> headers.add("Content-Type", "application/json"), "{\"input\":1}"));
    }

    @Test
    void callNonBlockingGetAPI_emitsBodyOnSuccess() {
        TestAPIService service = this.serviceWithResponse(200, "{\"price\":1.23}");

        String body = service.callNonBlockingGetAPI("https://api.example.com/test", String.class, headers -> { }).block();

        assertEquals("{\"price\":1.23}", body);
    }

    @Test
    void callNonBlockingGetAPI_throwsAPIExceptionOnErrorStatus() {
        TestAPIService service = this.serviceWithResponse(500, "error");

        assertThrows(APIException.class, () -> service.callNonBlockingGetAPI("https://api.example.com/test", String.class, headers -> { }).block());
    }

    @Test
    void checkForEmptyBody_throwsWhenResponseIsNull() {
        TestAPIService service = this.serviceWithResponse(200, "{}");

        assertThrows(APIException.class, () -> service.checkForEmptyBody(null));
    }

    @Test
    void checkForEmptyBody_throwsWhenBodyIsMissing() {
        TestAPIService service = this.serviceWithResponse(200, "{}");

        assertThrows(APIException.class, () -> service.checkForEmptyBody(ResponseEntity.ok().build()));
    }

    @Test
    void checkForEmptyBody_doesNotThrowWhenBodyPresent() {
        TestAPIService service = this.serviceWithResponse(200, "{}");

        service.checkForEmptyBody(ResponseEntity.ok("body"));
    }

    @Test
    void getJsonNodeFromBody_parsesValidJson() {
        TestAPIService service = this.serviceWithResponse(200, "{}");

        JsonNode node = service.getJsonNodeFromBody("{\"price\":1.23}");

        assertEquals(1.23, node.get("price").asDouble());
    }

    @Test
    void getJsonNodeFromBody_throwsAPIExceptionOnInvalidJson() {
        TestAPIService service = this.serviceWithResponse(200, "{}");

        assertThrows(APIException.class, () -> service.getJsonNodeFromBody("not json"));
    }

    private static class TestAPIService extends APIService {
        TestAPIService(CredentialRepository credentialRepository, ConfigRepository configRepository,
                        WebClient webClient, ObjectMapper objectMapper) {
            super(credentialRepository, configRepository, webClient, objectMapper);
        }
    }
}
