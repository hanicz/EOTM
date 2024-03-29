package eye.on.the.money.controller;

import eye.on.the.money.dto.out.CandleQuoteDTO;
import eye.on.the.money.model.stock.Exchange;
import eye.on.the.money.model.stock.Stock;
import eye.on.the.money.model.stock.Symbol;
import eye.on.the.money.service.stock.StockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("api/v1/stock")
@Slf4j
@RequiredArgsConstructor
public class StockController {

    private final StockService stockService;


    @GetMapping()
    public ResponseEntity<List<Stock>> getAllStocks() {
        log.trace("Enter");
        return new ResponseEntity<>(this.stockService.getAllStocks(), HttpStatus.OK);
    }

    @GetMapping("symbols/{exchange}")
    public ResponseEntity<List<Symbol>> getAllSymbols(@PathVariable String exchange) {
        log.trace("Enter");
        return new ResponseEntity<>(this.stockService.getAllSymbols(exchange), HttpStatus.OK);
    }

    @GetMapping("exchanges")
    public ResponseEntity<List<Exchange>> getAllExchanges() {
        log.trace("Enter");
        return new ResponseEntity<>(this.stockService.getAllExchanges(), HttpStatus.OK);
    }

    @GetMapping("candle/{shortName}/{months}")
    public ResponseEntity<CandleQuoteDTO> getCandleQuoteByShortName(@PathVariable String shortName, @PathVariable int months) {
        log.trace("Enter");
        return new ResponseEntity<>(this.stockService.getCandleQuoteByShortName(shortName, months), HttpStatus.OK);
    }
}
