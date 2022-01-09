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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class StockMetricAPIServiceImpl implements StockMetricAPIService {

    @Autowired
    private CredentialRepository credentialRepository;

    @Autowired
    private ConfigRepository configRepository;

    @Override
    public Profile getProfile(String symbol) {
        String metricAPI = this.configRepository.findById("finnhub").orElseThrow(NoSuchElementException::new).getConfigValue();
        String secret = this.credentialRepository.findById("finnhub").orElseThrow(NoSuchElementException::new).getSecret();
        String URL = MessageFormat.format(metricAPI + "/stock/profile2?symbol={0}&token={1}", symbol, secret);
        ResponseEntity<?> response = this.callStockMetricAPI(URL, Profile.class);
        return (Profile) response.getBody();
    }

    @Override
    public String[] getPeers(String symbol) {
        String metricAPI = this.configRepository.findById("finnhub").orElseThrow(NoSuchElementException::new).getConfigValue();
        String secret = this.credentialRepository.findById("finnhub").orElseThrow(NoSuchElementException::new).getSecret();
        String URL = MessageFormat.format(metricAPI + "/stock/peers?symbol={0}&token={1}", symbol, secret);
        ResponseEntity<?> response = this.callStockMetricAPI(URL, String[].class);
        return (String[]) response.getBody();
    }

    @Override
    public Metric getMetric(String symbol) {
        String metricAPI = this.configRepository.findById("finnhub").orElseThrow(NoSuchElementException::new).getConfigValue();
        String secret = this.credentialRepository.findById("finnhub").orElseThrow(NoSuchElementException::new).getSecret();
        String URL = MessageFormat.format(metricAPI + "/stock/metric?metric=all&symbol={0}&token={1}", symbol, secret);
        ResponseEntity<?> response = this.callStockMetricAPI(URL, String.class);
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode metric = mapper.readTree((String) response.getBody()).path("metric");
            return mapper.treeToValue(metric, Metric.class);
        } catch (JsonProcessingException | NullPointerException e) {
            throw new APIException("JSON process failed");
        }
    }

    @Override
    public List<Recommendation> getRecommendations(String symbol) {
        String metricAPI = this.configRepository.findById("finnhub").orElseThrow(NoSuchElementException::new).getConfigValue();
        String secret = this.credentialRepository.findById("finnhub").orElseThrow(NoSuchElementException::new).getSecret();
        String URL = MessageFormat.format(metricAPI + "/stock/recommendation?symbol={0}&token={1}", symbol, secret);
        ResponseEntity<?> response = this.callStockMetricAPI(URL, Recommendation[].class);
        return Arrays.asList((Recommendation[]) response.getBody());
    }

    @Retryable(value = APIException.class, maxAttempts = 3)
    private ResponseEntity<?> callStockMetricAPI(String URL, Class<?> cls) {
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<?> response = restTemplate.getForEntity(URL, cls);
        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            return response;
        } else {
            throw new APIException("Unable to reach currency API");
        }
    }
}
