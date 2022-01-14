package eye.on.the.money.service.impl;

import eye.on.the.money.model.stock.CandleQuote;
import eye.on.the.money.model.stock.Stock;
import eye.on.the.money.repository.stock.StockRepository;
import eye.on.the.money.service.StockService;
import eye.on.the.money.service.api.StockAPIService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StockServiceImpl implements StockService {

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private StockAPIService stockAPIService;

    @Override
    public List<Stock> getAllStocks() {
        return this.stockRepository.findAllByOrderByShortNameAsc();
    }

    @Override
    public CandleQuote getCandleQuoteByShortName(String shortName, int months) {
        CandleQuote cq = this.stockAPIService.getCandleQuoteByShortName(shortName, months);
        if (!cq.sameSize()) {
            throw new RuntimeException("Invalid candle data");
        }
        return cq;
    }
}
