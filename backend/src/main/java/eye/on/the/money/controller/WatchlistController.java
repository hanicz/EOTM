package eye.on.the.money.controller;

import eye.on.the.money.dto.out.CryptoWatchDTO;
import eye.on.the.money.dto.out.ForexWatchDTO;
import eye.on.the.money.dto.out.StockWatchDTO;
import eye.on.the.money.model.User;
import eye.on.the.money.service.WatchlistService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("watchlist")
@Slf4j
public class WatchlistController {

    @Autowired
    private WatchlistService watchlistService;

    @GetMapping("/crypto/{currency}")
    public ResponseEntity<List<CryptoWatchDTO>> getCryptoWatchList(@AuthenticationPrincipal User user, @PathVariable String currency) {
        log.trace("Enter getCryptoWatchList");
        List<CryptoWatchDTO> cryptoWatchList = this.watchlistService.getCryptoWatchlistByUserId(user.getId(), currency);
        cryptoWatchList.sort(Collections.reverseOrder());
        return new ResponseEntity<List<CryptoWatchDTO>>(cryptoWatchList, HttpStatus.OK);
    }

    @GetMapping("/forex")
    public ResponseEntity<List<ForexWatchDTO>> getForexWatchList(@AuthenticationPrincipal User user) {
        log.trace("Enter getForexWatchList");
        return new ResponseEntity<List<ForexWatchDTO>>(this.watchlistService.getForexWatchlistByUserId(user.getId()), HttpStatus.OK);
    }

    @GetMapping("/stock")
    public ResponseEntity<List<StockWatchDTO>> getStockWatchList(@AuthenticationPrincipal User user) {
        log.trace("Enter getStockWatchList");
        return new ResponseEntity<List<StockWatchDTO>>(this.watchlistService.getStockWatchlistByUserId(user.getId()), HttpStatus.OK);
    }

    @DeleteMapping("/crypto/{id}")
    public ResponseEntity<HttpStatus> deleteCryptoWatch(@AuthenticationPrincipal User user, @PathVariable Long id) {
        log.trace("Enter deleteCryptoWatch");
        this.watchlistService.deleteCryptoWatchById(user.getId(), id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @DeleteMapping("/stock/{id}")
    public ResponseEntity<HttpStatus> deleteStockWatch(@AuthenticationPrincipal User user, @PathVariable Long id) {
        log.trace("Enter deleteStockWatch");
        this.watchlistService.deleteStockWatchById(user.getId(), id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @DeleteMapping("/forex/{id}")
    public ResponseEntity<HttpStatus> deleteForexWatch(@AuthenticationPrincipal User user, @PathVariable Long id) {
        log.trace("Enter deleteForexWatch");
        this.watchlistService.deleteForexWatchById(user.getId(), id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PostMapping("/stock/{stockId}")
    public ResponseEntity<StockWatchDTO> createStockWatch(@AuthenticationPrincipal User user, @PathVariable String stockId) {
        log.trace("Enter createStockWatch");
        return new ResponseEntity<StockWatchDTO>(this.watchlistService.createNewStockWatch(user, stockId), HttpStatus.OK);
    }

    @PostMapping("/crypto/{coinId}")
    public ResponseEntity<CryptoWatchDTO> createCryptoWatch(@AuthenticationPrincipal User user, @PathVariable String coinId) {
        log.trace("Enter createCryptoWatch");
        return new ResponseEntity<CryptoWatchDTO>(this.watchlistService.createNewCryptoWatch(user, coinId), HttpStatus.OK);
    }
}
