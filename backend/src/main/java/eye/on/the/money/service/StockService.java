package eye.on.the.money.service;

import eye.on.the.money.model.stock.CandleQuote;
import eye.on.the.money.model.stock.Stock;
import eye.on.the.money.model.stock.Symbol;

import java.util.List;

public interface StockService {
    public List<Stock> getAllStocks();
    public CandleQuote getCandleQuoteByShortName(String shortName, int months);
    public List<Symbol> getAllSymbols();
}
