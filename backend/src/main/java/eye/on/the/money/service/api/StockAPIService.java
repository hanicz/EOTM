package eye.on.the.money.service.api;

import eye.on.the.money.dto.out.InvestmentDTO;
import eye.on.the.money.dto.out.StockWatchDTO;
import eye.on.the.money.model.stock.CandleQuote;

import java.util.List;

public interface StockAPIService {
    public void getLiveValue(List<InvestmentDTO> investmentDTOList);
    public void getStockWatchList(List<StockWatchDTO> stockWatchList);
    public CandleQuote getCandleQuoteByShortName(String shortname, int months);
}
