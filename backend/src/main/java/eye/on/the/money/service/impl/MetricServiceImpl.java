package eye.on.the.money.service.impl;

import eye.on.the.money.model.stock.Metric;
import eye.on.the.money.model.stock.Profile;
import eye.on.the.money.model.stock.Recommendation;
import eye.on.the.money.service.MetricService;
import eye.on.the.money.service.api.StockMetricAPIService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class MetricServiceImpl implements MetricService {

    @Autowired
    private StockMetricAPIService stockMetricAPIService;

    @Override
    public Profile getProfileBySymbol(String symbol) {
        Profile profile = this.stockMetricAPIService.getProfile(symbol);
        profile.setPeers(Arrays.asList(this.stockMetricAPIService.getPeers(symbol)));
        return profile;
    }

    @Override
    public Metric getMetricBySymbol(String symbol) {
        return this.stockMetricAPIService.getMetric(symbol);
    }

    @Override
    public List<Recommendation> getRecommendations(String symbol) {
        return this.stockMetricAPIService.getRecommendations(symbol);
    }
}
