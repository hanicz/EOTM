package eye.on.the.money.service.stock;

import com.fasterxml.jackson.databind.JsonNode;
import eye.on.the.money.dto.in.EODCandleQuoteDTO;
import eye.on.the.money.dto.out.CandleQuoteDTO;
import eye.on.the.money.model.stock.Exchange;
import eye.on.the.money.model.stock.Stock;
import eye.on.the.money.model.stock.Symbol;
import eye.on.the.money.repository.stock.StockRepository;
import eye.on.the.money.service.api.EODAPIService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

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

        JsonNode responseBody = this.eodAPIService.getLiveValueForSingle(shortName, "/real-time/{1}/?api_token={0}&fmt=json&");
        boolean sameDay = this.sameDay(eodList.get(eodList.size() - 1).getDate(), responseBody.findValue("timestamp").longValue());
        int arraySize = sameDay ? eodList.size() : eodList.size() + 1;

        return CandleQuoteDTO.createFromEODResponse(arraySize, eodList, sameDay ? null : responseBody);
    }

    private boolean sameDay(LocalDate lastInCandleList, Long timeStampOnLiveValue) {
        LocalDate timeStampToLocalDate = Instant.ofEpochSecond(timeStampOnLiveValue).atZone(ZoneId.systemDefault()).toLocalDate();
        return lastInCandleList.equals(timeStampToLocalDate);
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
