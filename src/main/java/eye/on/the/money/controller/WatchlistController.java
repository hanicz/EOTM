package eye.on.the.money.controller;

import eye.on.the.money.dto.in.TransactionQuery;
import eye.on.the.money.dto.out.CryptoWatchDTO;
import eye.on.the.money.dto.out.ForexWatchDTO;
import eye.on.the.money.dto.out.StockWatchDTO;
import eye.on.the.money.dto.out.TransactionDTO;
import eye.on.the.money.model.User;
import eye.on.the.money.service.WatchlistService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("watchlist")
public class WatchlistController {

    private static final Logger log = LoggerFactory.getLogger(WatchlistController.class);

    @Autowired
    private WatchlistService watchlistService;

    @GetMapping("/crypto/{currency}")
    public ResponseEntity<List<CryptoWatchDTO>> getCryptoWatchList(@AuthenticationPrincipal User user, @PathVariable String currency){
        log.trace("Enter getCryptoWatchList");
        return new ResponseEntity<List<CryptoWatchDTO>>(this.watchlistService.getCryptoWatchlistByUserId(user.getId(), currency), HttpStatus.OK);
    }

    @GetMapping("/forex")
    public ResponseEntity<List<ForexWatchDTO>> getForexWatchList(@AuthenticationPrincipal User user){
        log.trace("Enter getForexWatchList");
        return new ResponseEntity<List<ForexWatchDTO>>(this.watchlistService.getForexWatchlistByUserId(user.getId()), HttpStatus.OK);
    }

    @GetMapping("/stock")
    public ResponseEntity<List<StockWatchDTO>> getStockWatchList(@AuthenticationPrincipal User user){
        log.trace("Enter getStockWatchList");
        return new ResponseEntity<List<StockWatchDTO>>(this.watchlistService.getStockWatchlistByUserId(user.getId()), HttpStatus.OK);
    }
}
