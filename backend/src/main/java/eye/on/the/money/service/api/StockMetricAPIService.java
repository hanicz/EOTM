package eye.on.the.money.service.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eye.on.the.money.exception.APIException;
import eye.on.the.money.model.stock.Metric;
import eye.on.the.money.model.stock.Profile;
import eye.on.the.money.model.stock.Recommendation;
import eye.on.the.money.repository.ConfigRepository;
import eye.on.the.money.repository.CredentialRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
public class StockMetricAPIService extends APIService {

    private final static String API = "finnhub";

    @Autowired
    public StockMetricAPIService(CredentialRepository credentialRepository, ConfigRepository configRepository,
                                 WebClient webClient, ObjectMapper mapper) {
        super(credentialRepository, configRepository, webClient, mapper);
    }

    @Retryable(retryFor = APIException.class, maxAttempts = 3)
    public Profile getProfile(String symbol) {
        log.trace("Enter");
        ResponseEntity<?> response = this.callGetAPI(this.createURL(StockMetricAPIService.API,
                "/stock/profile2?symbol={1}&token={0}", symbol), Profile.class);
        return (Profile) response.getBody();
    }

    @Retryable(retryFor = APIException.class, maxAttempts = 3)
    public String[] getPeers(String symbol) {
        log.trace("Enter");
        ResponseEntity<?> response = this.callGetAPI(this.createURL(StockMetricAPIService.API,
                "/stock/peers?symbol={1}&token={0}", symbol), String[].class);
        return (String[]) response.getBody();
    }

    @Retryable(retryFor = APIException.class, maxAttempts = 3)
    public Metric getMetric(String symbol) {
        log.trace("Enter");
        ResponseEntity<?> response = this.callGetAPI(this.createURL(StockMetricAPIService.API,
                "/stock/metric?metric=all&symbol={1}&token={0}", symbol), String.class);
        try {
            JsonNode metric = this.mapper.readTree((String) response.getBody()).path("metric");
            return this.mapper.treeToValue(metric, Metric.class);
        } catch (JsonProcessingException | NullPointerException e) {
            log.error("JSON process failed. {}", e.getMessage());
            throw new APIException("JSON process failed");
        }
    }

    @Retryable(retryFor = APIException.class, maxAttempts = 3)
    public List<Recommendation> getRecommendations(String symbol) {
        log.trace("Enter");
        ResponseEntity<?> response = this.callGetAPI(this.createURL(StockMetricAPIService.API,
                "/stock/recommendation?symbol={1}&token={0}", symbol), Recommendation[].class);
        return Arrays.asList((Recommendation[]) response.getBody());
    }
}
