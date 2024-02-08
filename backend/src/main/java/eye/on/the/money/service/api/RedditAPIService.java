package eye.on.the.money.service.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eye.on.the.money.repository.ConfigRepository;
import eye.on.the.money.repository.CredentialRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.text.MessageFormat;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Consumer;
import java.util.stream.Stream;

@Service
@Slf4j
public class RedditAPIService extends APIService {

    private final static String API = "reddit";
    private final static String TOKEN_API = "redditToken";

    @Autowired
    public RedditAPIService(CredentialRepository credentialRepository, ConfigRepository configRepository,
                            WebClient webClient, ObjectMapper mapper) {
        super(credentialRepository, configRepository, webClient, mapper);
    }

    @Cacheable("token")
    public JsonNode getToken() {
        String url = this.createURL(RedditAPIService.TOKEN_API, "access_token");
        log.trace(url);
        ResponseEntity<?> response = this.callPostAPI(url, String.class,
                this.getBasicHttpHeader(), this.getFormMap());
        return this.getJsonNodeFromBody((String) response.getBody());
    }

    public Flux<JsonNode> getHotRedditNews(List<String> subreddits, String bearerToken) {
        return Flux.fromIterable(subreddits).flatMap(s ->
                this.callNonBlockingGetAPI(this.createURL(RedditAPIService.API, "{0}/hot.json?limit=10", s),
                        JsonNode.class, this.getBearerHttpHeader(bearerToken)));
    }

    private Consumer<HttpHeaders> getBasicHttpHeader() {
        return headers -> {
            headers.add("Authorization", "Basic " +
                    this.credentialRepository.findById(RedditAPIService.API).orElseThrow(NoSuchElementException::new).getSecret());
        };
    }

    private Consumer<HttpHeaders> getBearerHttpHeader(String bearerToken) {
        return headers -> {
            headers.add("Authorization", "Bearer  " + bearerToken);
        };
    }

    private MultiValueMap<String, String> getFormMap() {
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("grant_type", "client_credentials");
        return map;
    }

    @Override
    protected String createURL(String api, String path, String... params) {
        String apiURL = this.configRepository.findById(api).orElseThrow(NoSuchElementException::new).getConfigValue();
        Object[] array = Stream.of(params).toArray();

        return MessageFormat.format(apiURL + path, array);
    }
}
