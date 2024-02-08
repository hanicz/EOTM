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
import reactor.core.publisher.Mono;

import java.net.URI;
import java.text.MessageFormat;
import java.util.NoSuchElementException;
import java.util.function.Consumer;
import java.util.stream.Stream;

@Slf4j
@RequiredArgsConstructor
public abstract class APIService {

    protected final CredentialRepository credentialRepository;
    protected final ConfigRepository configRepository;
    protected final WebClient webClient;
    protected final ObjectMapper objectMapper;

    protected String createURL(String api, String path, String... params) {
        String stockAPI = this.configRepository.findById(api).orElseThrow(NoSuchElementException::new).getConfigValue();
        Object secret = this.credentialRepository.findById(api).orElseThrow(NoSuchElementException::new).getSecret();
        Object[] array = Stream.concat(Stream.of(secret), Stream.of(params)).toArray();

        return MessageFormat.format(stockAPI + path, array);
    }

    protected <T> ResponseEntity<T> callGetAPI(String URL, Class<T> cls) {
        log.trace("Call to {}", URL);
        ResponseEntity<T> responseEntity = this.webClient
                .get()
                .uri(URI.create(URL))
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> {
                    throw new APIException("Unable to make GET call" + response.statusCode());
                })
                .toEntity(cls)
                .block();
        this.checkForEmptyBody(responseEntity);

        return responseEntity;
    }

    protected <T> Mono<T> callNonBlockingGetAPI(String URL, Class<T> cls, Consumer<HttpHeaders> headersConsumer) {
        log.info("Call to {}", URL);
        return this.webClient.get()
                .uri(URI.create(URL))
                .headers(headersConsumer)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> {
                    throw new APIException("Unable to make GET call" + response.statusCode());
                })
                .bodyToMono(cls);
    }

    protected <T> ResponseEntity<T> callPostAPI(String URL, Class<T> cls, Consumer<HttpHeaders> headersConsumer, Object body) {
        log.trace("Call to {}", URL);
        ResponseEntity<T> responseEntity = this.webClient
                .post()
                .uri(URI.create(URL))
                .headers(headersConsumer)
                .bodyValue(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> {
                    throw new APIException("Unable to make POST call" + response.statusCode());
                })
                .toEntity(cls)
                .block();
        this.checkForEmptyBody(responseEntity);

        return responseEntity;
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
