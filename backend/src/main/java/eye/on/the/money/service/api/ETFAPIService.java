package eye.on.the.money.service.api;

import eye.on.the.money.model.etf.ETF;

import java.util.List;

public interface ETFAPIService {
    public void updateETFPrices(List<ETF> etfList);
}
