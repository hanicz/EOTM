package eye.on.the.money.controller;

import eye.on.the.money.model.User;
import eye.on.the.money.model.stock.CandleQuote;
import eye.on.the.money.model.stock.Stock;
import eye.on.the.money.model.stock.Symbol;
import eye.on.the.money.service.StockService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("stock")
@Slf4j
public class StockController {

    @Autowired
    private StockService stockService;

    @GetMapping()
    public ResponseEntity<List<Stock>> getAllStocks(@AuthenticationPrincipal User user) {
        log.trace("Enter getAllStocks");
        return new ResponseEntity<List<Stock>>(this.stockService.getAllStocks(), HttpStatus.OK);
    }

    @GetMapping("symbols")
    public ResponseEntity<List<Symbol>> getAllSymbols(@AuthenticationPrincipal User user) {
        log.trace("Enter getAllSymbols");
        return new ResponseEntity<List<Symbol>>(this.stockService.getAllSymbols(), HttpStatus.OK);
    }

    @GetMapping("candle/{shortName}/{months}")
    public ResponseEntity<CandleQuote> getCandleQuoteByShortName(@AuthenticationPrincipal User user, @PathVariable String shortName, @PathVariable int months) {
        log.trace("Enter getCandleQuoteByShortName");
        return new ResponseEntity<CandleQuote>(this.stockService.getCandleQuoteByShortName(shortName, months), HttpStatus.OK);
    }
}
