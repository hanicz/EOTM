package eye.on.the.money.service;

import eye.on.the.money.model.stock.Metric;
import eye.on.the.money.model.stock.Profile;

public interface MetricService {
    public Profile getProfileBySymbol(String symbol);
    public Metric getMetricBySymbol(String symbol);
}
