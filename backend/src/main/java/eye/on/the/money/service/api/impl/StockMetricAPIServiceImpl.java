package eye.on.the.money.service.api.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eye.on.the.money.exception.APIException;
import eye.on.the.money.model.stock.Metric;
import eye.on.the.money.model.stock.Profile;
import eye.on.the.money.model.stock.Recommendation;
import eye.on.the.money.repository.ConfigRepository;
import eye.on.the.money.repository.CredentialRepository;
import eye.on.the.money.service.api.StockMetricAPIService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@Slf4j
public class StockMetricAPIServiceImpl implements StockMetricAPIService {

    @Autowired
    private CredentialRepository credentialRepository;

    @Autowired
    private ConfigRepository configRepository;

    @Override
    @Retryable(value = APIException.class, maxAttempts = 3)
    public Profile getProfile(String symbol) {
        log.trace("Enter getProfile");
        String metricAPI = this.configRepository.findById("finnhub").orElseThrow(NoSuchElementException::new).getConfigValue();
        String secret = this.credentialRepository.findById("finnhub").orElseThrow(NoSuchElementException::new).getSecret();
        String URL = MessageFormat.format(metricAPI + "/stock/profile2?symbol={0}&token={1}", symbol, secret);
        ResponseEntity<?> response = this.callStockMetricAPI(URL, Profile.class);
        return (Profile) response.getBody();
    }

    @Override
    @Retryable(value = APIException.class, maxAttempts = 3)
    public String[] getPeers(String symbol) {
        log.trace("Enter getPeers");
        String metricAPI = this.configRepository.findById("finnhub").orElseThrow(NoSuchElementException::new).getConfigValue();
        String secret = this.credentialRepository.findById("finnhub").orElseThrow(NoSuchElementException::new).getSecret();
        String URL = MessageFormat.format(metricAPI + "/stock/peers?symbol={0}&token={1}", symbol, secret);
        ResponseEntity<?> response = this.callStockMetricAPI(URL, String[].class);
        return (String[]) response.getBody();
    }

    @Override
    @Retryable(value = APIException.class, maxAttempts = 3)
    public Metric getMetric(String symbol) {
        log.trace("Enter getMetric");
        String metricAPI = this.configRepository.findById("finnhub").orElseThrow(NoSuchElementException::new).getConfigValue();
        String secret = this.credentialRepository.findById("finnhub").orElseThrow(NoSuchElementException::new).getSecret();
        String URL = MessageFormat.format(metricAPI + "/stock/metric?metric=all&symbol={0}&token={1}", symbol, secret);
        ResponseEntity<?> response = this.callStockMetricAPI(URL, String.class);
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode metric = mapper.readTree((String) response.getBody()).path("metric");
            return mapper.treeToValue(metric, Metric.class);
        } catch (JsonProcessingException | NullPointerException e) {
            log.error("JSON process failed. " + e.getMessage());
            throw new APIException("JSON process failed");
        }
    }

    @Override
    @Retryable(value = APIException.class, maxAttempts = 3)
    public List<Recommendation> getRecommendations(String symbol) {
        log.trace("Enter getRecommendations");
        String metricAPI = this.configRepository.findById("finnhub").orElseThrow(NoSuchElementException::new).getConfigValue();
        String secret = this.credentialRepository.findById("finnhub").orElseThrow(NoSuchElementException::new).getSecret();
        String URL = MessageFormat.format(metricAPI + "/stock/recommendation?symbol={0}&token={1}", symbol, secret);
        ResponseEntity<?> response = this.callStockMetricAPI(URL, Recommendation[].class);
        return Arrays.asList((Recommendation[]) response.getBody());
    }

    private ResponseEntity<?> callStockMetricAPI(String URL, Class<?> cls) {
        RestTemplate restTemplate = new RestTemplate();
        try {
            ResponseEntity<?> response = restTemplate.getForEntity(URL, cls);
            if (response.getBody() != null) {
                return response;
            } else {
                log.error("Empty response from metric API");
                throw new APIException("Empty response from metric API");
            }
        } catch (RestClientException e) {
            log.error("Unable to reach metric API: " + e.getMessage());
            throw new APIException("Unable to reach metric API");
        }
    }
}
