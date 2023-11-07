package eye.on.the.money.service.stock.impl;

import eye.on.the.money.model.stock.*;
import eye.on.the.money.repository.stock.StockRepository;
import eye.on.the.money.service.api.EODAPIService;
import eye.on.the.money.service.stock.StockService;
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
    private EODAPIService eodAPIService;

    @Override
    public List<Stock> getAllStocks() {
        log.trace("Enter getAllStocks");
        return this.stockRepository.findAllByOrderByShortNameAsc();
    }

    @Override
    public List<Symbol> getAllSymbols(String exchange) {
        log.trace("Enter getAllSymbols");
        return this.eodAPIService.getAllSymbols(exchange);
    }

    @Override
    public List<Exchange> getAllExchanges() {
        log.trace("Enter getAllExchanges");
        return this.eodAPIService.getAllExchanges();
    }

    @Override
    public CandleQuote getCandleQuoteByShortName(String shortName, int months) {
        log.trace("Enter getCandleQuoteByShortName");
        List<EODCandleQuote> eodList = this.eodAPIService.getCandleQuoteByShortName(shortName, months);

        Double[] c = new Double[eodList.size()];
        Double[] o = new Double[eodList.size()];
        Double[] l = new Double[eodList.size()];
        Double[] h = new Double[eodList.size()];
        Long[] v = new Long[eodList.size()];
        Long[] t = new Long[eodList.size()];

        for (int i = 0; i < eodList.size(); i++) {
            c[i] = eodList.get(i).getClose();
            o[i] = eodList.get(i).getOpen();
            l[i] = eodList.get(i).getLow();
            h[i] = eodList.get(i).getHigh();
            v[i] = eodList.get(i).getVolume();
            t[i] = eodList.get(i).getDate().getTime();
        }

        return new CandleQuote(c, h, l, o, t, v);
    }
}
