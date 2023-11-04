package eye.on.the.money.service.stock;

import eye.on.the.money.model.stock.*;

import java.util.List;

public interface StockService {
    public List<Stock> getAllStocks();
    public CandleQuote getCandleQuoteByShortName(String shortName, int months);
    public List<Symbol> getAllSymbols(String exchange);
    public List<Exchange> getAllExchanges();
}
