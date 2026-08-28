package eye.on.the.money.service.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eye.on.the.money.exception.APIException;
import eye.on.the.money.repository.ConfigRepository;
import eye.on.the.money.repository.CredentialRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriUtils;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Stream;

@Slf4j
@RequiredArgsConstructor
public abstract class APIService {

    protected final CredentialRepository credentialRepository;
    protected final ConfigRepository configRepository;
    protected final WebClient webClient;
    protected final ObjectMapper objectMapper;

    private static final String SERVICE_UNAVAILABLE_MESSAGE = "Service temporarily unavailable";

    private final Map<String, String> apiUrlCache = new ConcurrentHashMap<>();
    private final Map<String, String> secretCache = new ConcurrentHashMap<>();

    protected String getApiUrl(String api) {
        return this.apiUrlCache.computeIfAbsent(api, key -> this.configRepository.findById(key)
                .orElseThrow(() -> this.missingApiSetting("config", key)).getConfigValue());
    }

    protected String getSecret(String api) {
        return this.secretCache.computeIfAbsent(api, key -> this.credentialRepository.findById(key)
                .orElseThrow(() -> this.missingApiSetting("credential", key)).getSecret());
    }

    private NoSuchElementException missingApiSetting(String kind, String key) {
        log.error("Missing API {} for key {}", kind, key);
        return new NoSuchElementException(SERVICE_UNAVAILABLE_MESSAGE);
    }

    protected String createURL(String api, String path, String... params) {
        Object[] array = Stream.concat(Stream.of(this.encodeQuery(this.getSecret(api))), Stream.of(params)).toArray();
        return this.expandTemplate(this.getApiUrl(api) + path, array);
    }

    protected String expandTemplate(String template, Object[] args) {
        String url = template;
        for (int index = 0; index < args.length; index++) {
            url = url.replace("{" + index + "}", String.valueOf(args[index]));
        }
        return url;
    }

    protected String encodePath(String value) {
        return UriUtils.encodePathSegment(value, StandardCharsets.UTF_8);
    }

    protected String encodeQuery(String value) {
        return UriUtils.encodeQueryParam(value, StandardCharsets.UTF_8);
    }

    protected <T> ResponseEntity<T> callGetAPI(String URL, Class<T> cls) {
        log.trace("Call to {}", this.endpoint(URL));
        long start = System.nanoTime();
        ResponseEntity<T> responseEntity;
        try {
            responseEntity = this.webClient
                    .get()
                    .uri(URI.create(URL))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, response -> {
                        log.error("GET call to {} failed with status {}", this.endpoint(URL), response.statusCode());
                        throw new APIException("Unable to make GET call" + response.statusCode());
                    })
                    .toEntity(cls)
                    .block();
        } finally {
            this.logElapsed("GET", URL, start);
        }
        this.checkForEmptyBody(responseEntity);

        return responseEntity;
    }

    protected <T> Mono<T> callNonBlockingGetAPI(String URL, Class<T> cls, Consumer<HttpHeaders> headersConsumer) {
        log.trace("Call to {}", this.endpoint(URL));
        return Mono.defer(() -> {
            long start = System.nanoTime();
            return this.webClient.get()
                    .uri(URI.create(URL))
                    .headers(headersConsumer)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, response -> {
                        log.error("GET call to {} failed with status {}", this.endpoint(URL), response.statusCode());
                        throw new APIException("Unable to make GET call" + response.statusCode());
                    })
                    .bodyToMono(cls)
                    .doFinally(signal -> this.logElapsed("GET", URL, start));
        });
    }

    protected <T> ResponseEntity<T> callPostAPI(String URL, Class<T> cls, Consumer<HttpHeaders> headersConsumer, Object body) {
        log.trace("Call to {}", this.endpoint(URL));
        long start = System.nanoTime();
        ResponseEntity<T> responseEntity;
        try {
            responseEntity = this.webClient
                    .post()
                    .uri(URI.create(URL))
                    .headers(headersConsumer)
                    .bodyValue(body)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, response -> {
                        log.error("POST call to {} failed with status {}", this.endpoint(URL), response.statusCode());
                        throw new APIException("Unable to make POST call" + response.statusCode());
                    })
                    .toEntity(cls)
                    .block();
        } finally {
            this.logElapsed("POST", URL, start);
        }
        this.checkForEmptyBody(responseEntity);

        return responseEntity;
    }

    private void logElapsed(String method, String URL, long startNanos) {
        log.info("{} {} took {} ms", method, this.endpoint(URL), (System.nanoTime() - startNanos) / 1_000_000);
    }

    protected String endpoint(String URL) {
        try {
            URI uri = URI.create(URL);
            return (uri.getHost() == null ? "" : uri.getHost()) + (uri.getPath() == null ? "" : uri.getPath());
        } catch (IllegalArgumentException e) {
            return "(unparsed url)";
        }
    }

    protected void checkForEmptyBody(ResponseEntity<?> response) {
        if (response == null || !response.hasBody()) {
            log.error("Empty response API");
            throw new APIException("Empty response from API");
        }
    }

    protected JsonNode getJsonNodeFromBody(String body) {
        try {
            return this.objectMapper.readTree(body);
        } catch (JsonProcessingException e) {
            log.error("JSON process failed: {}", e.getMessage());
            throw new APIException("JSON process failed");
        }
    }
}
