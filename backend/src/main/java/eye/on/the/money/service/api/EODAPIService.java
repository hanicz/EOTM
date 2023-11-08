package eye.on.the.money.service.api;

import com.fasterxml.jackson.databind.JsonNode;
import eye.on.the.money.dto.out.*;
import eye.on.the.money.model.stock.EODCandleQuote;
import eye.on.the.money.model.stock.Exchange;
import eye.on.the.money.model.stock.Symbol;

import java.util.List;

public interface EODAPIService {
    public JsonNode getLiveValue(String tickerList, String path);
    public List<EODCandleQuote> getCandleQuoteByShortName(String shortname, int months);
    public List<Symbol> getAllSymbols(String exchange);
    public List<Exchange> getAllExchanges();
}
