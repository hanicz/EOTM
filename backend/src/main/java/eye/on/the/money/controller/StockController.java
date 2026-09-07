package eye.on.the.money.controller;

import eye.on.the.money.dto.out.CandleQuoteDTO;
import eye.on.the.money.dto.out.SignalDTO;
import eye.on.the.money.model.stock.Exchange;
import eye.on.the.money.model.stock.Stock;
import eye.on.the.money.model.stock.Symbol;
import eye.on.the.money.service.signal.SignalService;
import eye.on.the.money.service.stock.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("api/v1/stock")
@RequiredArgsConstructor
public class StockController {

    private final StockService stockService;
    private final SignalService signalService;


    @GetMapping()
    public ResponseEntity<List<Stock>> getAllStocks() {
        return ResponseEntity.ok(this.stockService.getAllStocks());
    }

    @GetMapping("symbols/{exchange}")
    public ResponseEntity<List<Symbol>> getAllSymbols(@PathVariable String exchange) {
        return ResponseEntity.ok(this.stockService.getAllSymbols(exchange));
    }

    @GetMapping("exchanges")
    public ResponseEntity<List<Exchange>> getAllExchanges() {
        return ResponseEntity.ok(this.stockService.getAllExchanges());
    }

    @GetMapping("candle/{shortName}/{months}")
    public ResponseEntity<CandleQuoteDTO> getCandleQuoteByShortName(@PathVariable String shortName, @PathVariable int months) {
        return ResponseEntity.ok(this.stockService.getCandleQuoteByShortName(shortName, months));
    }

    @GetMapping("{shortName}/signal")
    public ResponseEntity<SignalDTO> getSignal(@PathVariable String shortName) {
        return ResponseEntity.ok(this.signalService.evaluate(shortName));
    }
}
