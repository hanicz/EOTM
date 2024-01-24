package eye.on.the.money.service.api.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import eye.on.the.money.exception.APIException;
import eye.on.the.money.model.stock.Metric;
import eye.on.the.money.model.stock.Profile;
import eye.on.the.money.model.stock.Recommendation;
import eye.on.the.money.service.api.APIService;
import eye.on.the.money.service.api.StockMetricAPIService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
public class StockMetricAPIServiceImpl extends APIService implements StockMetricAPIService {

    private final static String API = "finnhub";

    @Override
    @Retryable(retryFor = APIException.class, maxAttempts = 3)
    public Profile getProfile(String symbol) {
        log.trace("Enter");
        ResponseEntity<?> response = this.callGetAPI(this.createURL(StockMetricAPIServiceImpl.API,
                "/stock/profile2?symbol={1}&token={0}", symbol), Profile.class);
        return (Profile) response.getBody();
    }

    @Override
    @Retryable(retryFor = APIException.class, maxAttempts = 3)
    public String[] getPeers(String symbol) {
        log.trace("Enter");
        ResponseEntity<?> response = this.callGetAPI(this.createURL(StockMetricAPIServiceImpl.API,
                "/stock/peers?symbol={1}&token={0}", symbol), String[].class);
        return (String[]) response.getBody();
    }

    @Override
    @Retryable(retryFor = APIException.class, maxAttempts = 3)
    public Metric getMetric(String symbol) {
        log.trace("Enter");
        ResponseEntity<?> response = this.callGetAPI(this.createURL(StockMetricAPIServiceImpl.API,
                "/stock/metric?metric=all&symbol={1}&token={0}", symbol), String.class);
        try {
            JsonNode metric = this.mapper.readTree((String) response.getBody()).path("metric");
            return this.mapper.treeToValue(metric, Metric.class);
        } catch (JsonProcessingException | NullPointerException e) {
            log.error("JSON process failed. {}", e.getMessage());
            throw new APIException("JSON process failed");
        }
    }

    @Override
    @Retryable(retryFor = APIException.class, maxAttempts = 3)
    public List<Recommendation> getRecommendations(String symbol) {
        log.trace("Enter");
        ResponseEntity<?> response = this.callGetAPI(this.createURL(StockMetricAPIServiceImpl.API,
                "/stock/recommendation?symbol={1}&token={0}", symbol), Recommendation[].class);
        return Arrays.asList((Recommendation[]) response.getBody());
    }
}
