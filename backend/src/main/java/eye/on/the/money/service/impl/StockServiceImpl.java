package eye.on.the.money.service.impl;

import eye.on.the.money.model.stock.CandleQuote;
import eye.on.the.money.model.stock.Stock;
import eye.on.the.money.model.stock.Symbol;
import eye.on.the.money.repository.stock.StockRepository;
import eye.on.the.money.service.StockService;
import eye.on.the.money.service.api.StockAPIService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class StockServiceImpl implements StockService {

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private StockAPIService stockAPIService;

    @Override
    public List<Stock> getAllStocks() {
        log.trace("Enter getAllStocks");
        return this.stockRepository.findAllByOrderByShortNameAsc();
    }

    @Override
    public List<Symbol> getAllSymbols() {
        log.trace("Enter getAllSymbols");
        return this.stockAPIService.getAllSymbols();
    }

    @Override
    public CandleQuote getCandleQuoteByShortName(String shortName, int months) {
        log.trace("Enter getCandleQuoteByShortName");
        CandleQuote cq = this.stockAPIService.getCandleQuoteByShortName(shortName, months);
        if (!cq.sameSize()) {
            log.error("CandleQuote size is not the same as expected");
            throw new RuntimeException("Invalid candle data");
        }
        return cq;
    }
}
