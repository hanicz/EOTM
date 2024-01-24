package eye.on.the.money.service.api;

import com.fasterxml.jackson.databind.JsonNode;
import eye.on.the.money.dto.out.*;
import eye.on.the.money.model.stock.EODCandleQuote;
import eye.on.the.money.model.stock.Exchange;
import eye.on.the.money.model.stock.Symbol;

import java.util.List;

public interface EODAPIService {
    JsonNode getLiveValue(String tickerList, String path);
    List<EODCandleQuote> getCandleQuoteByShortName(String shortname, int months);
    List<Symbol> getAllSymbols(String exchange);
    List<Exchange> getAllExchanges();
    JsonNode getLiveValueForSingle(String ticker, String path);
}
