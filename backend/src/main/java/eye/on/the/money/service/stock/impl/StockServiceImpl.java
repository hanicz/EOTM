package eye.on.the.money.service.stock.impl;

import com.fasterxml.jackson.databind.JsonNode;
import eye.on.the.money.model.stock.*;
import eye.on.the.money.repository.stock.StockRepository;
import eye.on.the.money.service.api.EODAPIService;
import eye.on.the.money.service.stock.StockService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class StockServiceImpl implements StockService {

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private EODAPIService eodAPIService;

    private final SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMdd");

    @Override
    public List<Stock> getAllStocks() {
        log.trace("Enter getAllStocks");
        return this.stockRepository.findAllByOrderByShortNameAsc();
    }

    @Override
    @Cacheable("symbols")
    public List<Symbol> getAllSymbols(String exchange) {
        log.trace("Enter getAllSymbols");
        return this.eodAPIService.getAllSymbols(exchange);
    }

    @CacheEvict(value = {"symbols", "exchanges"}, allEntries = true)
    @Scheduled(fixedRateString = "${cache.stockTTL}")
    public void evictStockRelatedCache() {
        log.trace("Evict stock related cache");
    }

    @Override
    @Cacheable("exchanges")
    public List<Exchange> getAllExchanges() {
        log.trace("Enter getAllExchanges");
        return this.eodAPIService.getAllExchanges();
    }

    @Override
    public CandleQuote getCandleQuoteByShortName(String shortName, int months) {
        log.trace("Enter getCandleQuoteByShortName");
        List<EODCandleQuote> eodList = this.eodAPIService.getCandleQuoteByShortName(shortName, months);

        JsonNode responseBody = this.eodAPIService.getLiveValueForSingle(shortName, "/real-time/{1}/?api_token={0}&fmt=json&");
        boolean sameDay = this.fmt.format(eodList.get(eodList.size() -1 ).getDate())
                .equals(this.fmt.format(new Date(TimeUnit.SECONDS.toMillis(responseBody.findValue("timestamp").longValue()))));
        int arraySize = sameDay ? eodList.size() : eodList.size() + 1;

        return CandleQuote.createFromEODResponse(arraySize, eodList, sameDay ? null : responseBody);
    }

    @Override
    public Stock getOrCreateStock(String shortName, String exchange, String name) {
        return this.stockRepository.findById(shortName.toLowerCase()).orElseGet(() -> {
                    Stock newStock = Stock.builder()
                            .id(shortName.toLowerCase())
                            .exchange(exchange)
                            .shortName(shortName.toUpperCase())
                            .name(name)
                            .build();
                    return this.stockRepository.save(newStock);
                }
        );
    }
}
