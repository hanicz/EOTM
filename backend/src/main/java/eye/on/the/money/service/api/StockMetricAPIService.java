package eye.on.the.money.service.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eye.on.the.money.dto.out.MetricDTO;
import eye.on.the.money.dto.out.ProfileDTO;
import eye.on.the.money.dto.out.RecommendationDTO;
import eye.on.the.money.exception.APIException;
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
    public ProfileDTO getProfile(String symbol) {
        log.trace("Enter");
        String url = this.createURL(StockMetricAPIService.API,
                "/stock/profile2?symbol={1}&token={0}", symbol);
        ResponseEntity<?> response = this.callGetAPI(url, ProfileDTO.class);
        return (ProfileDTO) response.getBody();
    }

    @Retryable(retryFor = APIException.class, maxAttempts = 3)
    public String[] getPeers(String symbol) {
        log.trace("Enter");
        String url = this.createURL(StockMetricAPIService.API,
                "/stock/peers?symbol={1}&token={0}", symbol);
        ResponseEntity<?> response = this.callGetAPI(url, String[].class);
        return (String[]) response.getBody();
    }

    @Retryable(retryFor = APIException.class, maxAttempts = 3)
    public MetricDTO getMetric(String symbol) {
        log.trace("Enter");
        String url = this.createURL(StockMetricAPIService.API,
                "/stock/metric?metric=all&symbol={1}&token={0}", symbol);
        ResponseEntity<?> response = this.callGetAPI(url, String.class);
        try {
            JsonNode metric = this.objectMapper.readTree((String) response.getBody()).path("metric");
            return this.objectMapper.treeToValue(metric, MetricDTO.class);
        } catch (JsonProcessingException | NullPointerException e) {
            log.error("JSON process failed. {}", e.getMessage());
            throw new APIException("JSON process failed");
        }
    }

    @Retryable(retryFor = APIException.class, maxAttempts = 3)
    public List<RecommendationDTO> getRecommendations(String symbol) {
        log.trace("Enter");
        ResponseEntity<?> response = this.callGetAPI(this.createURL(StockMetricAPIService.API,
                "/stock/recommendation?symbol={1}&token={0}", symbol), RecommendationDTO[].class);
        return Arrays.asList((RecommendationDTO[]) response.getBody());
    }
}
