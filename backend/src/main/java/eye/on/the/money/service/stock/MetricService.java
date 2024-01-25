package eye.on.the.money.service.stock;

import eye.on.the.money.model.stock.Metric;
import eye.on.the.money.model.stock.Profile;
import eye.on.the.money.model.stock.Recommendation;
import eye.on.the.money.service.api.StockMetricAPIService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@Service
@Slf4j
public class MetricService {

    private final StockMetricAPIService stockMetricAPIService;

    @Autowired
    public MetricService(StockMetricAPIService stockMetricAPIService) {
        this.stockMetricAPIService = stockMetricAPIService;
    }

    public Profile getProfileBySymbol(String symbol) {
        log.trace("Enter");
        Profile profile = this.stockMetricAPIService.getProfile(symbol);
        profile.setPeers(Arrays.asList(this.stockMetricAPIService.getPeers(symbol)));
        return profile;
    }

    public Metric getMetricBySymbol(String symbol) {
        log.trace("Enter");
        return this.stockMetricAPIService.getMetric(symbol);
    }

    public List<Recommendation> getRecommendations(String symbol) {
        log.trace("Enter");
        List<Recommendation> recommendations = this.stockMetricAPIService.getRecommendations(symbol);
        recommendations.sort(Comparator.comparing(Recommendation::getPeriod));
        return recommendations;
    }
}
