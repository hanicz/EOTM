package eye.on.the.money.service.api;

import eye.on.the.money.dto.out.*;
import eye.on.the.money.model.stock.EODCandleQuote;
import eye.on.the.money.model.stock.Exchange;
import eye.on.the.money.model.stock.Symbol;

import java.util.List;

public interface EODAPIService {
    public void getLiveValue(List<InvestmentDTO> investmentDTOList);
    public void getETFLiveValue(List<ETFInvestmentDTO> investmentDTOList);
    public void getStockWatchList(List<StockWatchDTO> stockWatchList);
    public void getForexWatchList(List<ForexWatchDTO> forexWatchList);
    public List<EODCandleQuote> getCandleQuoteByShortName(String shortname, int months);
    public List<Symbol> getAllSymbols(String exchange);
    public List<Exchange> getAllExchanges();
    public void changeLiveValueCurrencyForForexTransactions(List<ForexTransactionDTO> forexTransactions);
}
