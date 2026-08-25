package eye.on.the.money.service.stock;

import com.fasterxml.jackson.databind.JsonNode;
import eye.on.the.money.dto.in.EODCandleQuoteDTO;
import eye.on.the.money.dto.out.CandleQuoteDTO;
import eye.on.the.money.model.stock.Exchange;
import eye.on.the.money.model.stock.Stock;
import eye.on.the.money.model.stock.Symbol;
import eye.on.the.money.repository.stock.StockRepository;
import eye.on.the.money.service.api.EODAPIService;
import eye.on.the.money.util.LiveQuote;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.OptionalDouble;

@Service
@Slf4j
@RequiredArgsConstructor
public class StockService {

    private final StockRepository stockRepository;
    private final EODAPIService eodAPIService;

    public List<Stock> getAllStocks() {
        log.trace("Enter getAllStocks");
        return this.stockRepository.findAllByOrderByShortNameAsc();
    }

    @Cacheable("symbols")
    public List<Symbol> getAllSymbols(String exchange) {
        log.trace("Enter getAllSymbols");
        return this.eodAPIService.getAllSymbols(exchange);
    }

    @Cacheable("exchanges")
    public List<Exchange> getAllExchanges() {
        log.trace("Enter getAllExchanges");
        return this.eodAPIService.getAllExchanges();
    }

    public CandleQuoteDTO getCandleQuoteByShortName(String shortName, int months) {
        log.trace("Enter getCandleQuoteByShortName");
        List<EODCandleQuoteDTO> eodList = this.eodAPIService.getCandleQuoteByShortName(shortName, months);

        JsonNode responseBody = this.eodAPIService.getLiveValueForSingle(shortName);
        boolean appendLiveCandle = this.appendable(eodList, responseBody);
        int arraySize = appendLiveCandle ? eodList.size() + 1 : eodList.size();

        return CandleQuoteDTO.createFromEODResponse(arraySize, eodList, appendLiveCandle ? responseBody : null);
    }

    private boolean appendable(List<EODCandleQuoteDTO> eodList, JsonNode liveValue) {
        if (eodList.isEmpty()) return false;
        OptionalDouble timestamp = LiveQuote.numeric(liveValue, "timestamp");
        if (timestamp.isEmpty() || LiveQuote.price(liveValue).isEmpty()) {
            log.info("No usable live candle to append, charting historical quotes only");
            return false;
        }
        LocalDate liveDate = Instant.ofEpochSecond((long) timestamp.getAsDouble())
                .atZone(ZoneId.systemDefault()).toLocalDate();
        return !eodList.getLast().getDate().equals(liveDate);
    }

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
