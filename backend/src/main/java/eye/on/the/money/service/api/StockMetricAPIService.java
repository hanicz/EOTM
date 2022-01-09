package eye.on.the.money.service.api;

import eye.on.the.money.model.stock.Metric;
import eye.on.the.money.model.stock.Profile;
import eye.on.the.money.model.stock.Recommendation;

import java.util.List;

public interface StockMetricAPIService {
    public Profile getProfile(String symbol);

    public String[] getPeers(String symbol);

    public Metric getMetric(String symbol);

    public List<Recommendation> getRecommendations(String symbol);
}
