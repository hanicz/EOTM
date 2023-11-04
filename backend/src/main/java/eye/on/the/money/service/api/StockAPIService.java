package eye.on.the.money.service.api;

import eye.on.the.money.dto.out.InvestmentDTO;
import eye.on.the.money.dto.out.StockWatchDTO;
import eye.on.the.money.model.stock.EODCandleQuote;
import eye.on.the.money.model.stock.Exchange;
import eye.on.the.money.model.stock.Symbol;

import java.util.List;

public interface StockAPIService {
    public void getLiveValue(List<InvestmentDTO> investmentDTOList);
    public void getStockWatchList(List<StockWatchDTO> stockWatchList);
    public List<EODCandleQuote> getCandleQuoteByShortName(String shortname, int months);
    public List<Symbol> getAllSymbols(String exchange);

    public List<Exchange> getAllExchanges();
}
