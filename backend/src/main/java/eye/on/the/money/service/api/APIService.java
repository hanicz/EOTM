package eye.on.the.money.service.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eye.on.the.money.exception.APIException;
import eye.on.the.money.repository.ConfigRepository;
import eye.on.the.money.repository.CredentialRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.text.MessageFormat;
import java.util.NoSuchElementException;
import java.util.stream.Stream;

@Slf4j
public abstract class APIService {

    @Autowired
    private CredentialRepository credentialRepository;
    @Autowired
    private ConfigRepository configRepository;
    @Autowired
    protected WebClient webClient;
    @Autowired
    protected ObjectMapper mapper;

    protected String createURL(String api, String path, String... params) {
        log.trace("Enter");
        String stockAPI = this.configRepository.findById(api).orElseThrow(NoSuchElementException::new).getConfigValue();
        Object secret = this.credentialRepository.findById(api).orElseThrow(NoSuchElementException::new).getSecret();
        Object[] array = Stream.concat(Stream.of(secret), Stream.of(params)).toArray();

        String URL = MessageFormat.format(stockAPI + path, array);
        log.debug(URL);
        return URL;
    }

    protected ResponseEntity<?> callGetAPI(String URL, Class<?> cls) {
        log.trace("Enter");

        log.debug("Call to {}", URL);
        ResponseEntity<?> responseEntity = this.webClient
                .get()
                .uri(URI.create(URL))
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> response.bodyToMono(String.class)
                        .flatMap(error -> Mono.error(new APIException(error))))
                .toEntity(cls)
                .block();
        this.checkForEmptyBody(responseEntity);

        return responseEntity;
    }

    protected void checkForEmptyBody(ResponseEntity<?> response) {
        if(response == null || !response.hasBody()) {
            log.error("Empty response API");
            throw new APIException("Empty response from API");
        }
    }

    protected JsonNode getJsonNodeFromBody(String body) {
        try {
            return this.mapper.readTree(body);
        } catch (JsonProcessingException e) {
            log.error("JSON process failed: {}", e.getMessage());
            throw new APIException("JSON process failed");
        }
    }
}
