package eye.on.the.money.controller;

import eye.on.the.money.dto.out.CryptoWatchDTO;
import eye.on.the.money.dto.out.ForexWatchDTO;
import eye.on.the.money.dto.out.StockWatchDTO;
import eye.on.the.money.model.stock.Stock;
import eye.on.the.money.service.WatchlistService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
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
    public ResponseEntity<List<CryptoWatchDTO>> getCryptoWatchList(@AuthenticationPrincipal UserDetails user, @PathVariable String currency) {
        log.trace("Enter");
        List<CryptoWatchDTO> cryptoWatchList = this.watchlistService.getCryptoWatchlistByUserId(user.getUsername(), currency);
        cryptoWatchList.sort(Collections.reverseOrder());
        return new ResponseEntity<>(cryptoWatchList, HttpStatus.OK);
    }

    @GetMapping("/forex")
    public ResponseEntity<List<ForexWatchDTO>> getForexWatchList(@AuthenticationPrincipal UserDetails user) {
        log.trace("Enter");
        return new ResponseEntity<>(this.watchlistService.getForexWatchlistByUserId(user.getUsername()), HttpStatus.OK);
    }

    @GetMapping("/stock")
    public ResponseEntity<List<StockWatchDTO>> getStockWatchList(@AuthenticationPrincipal UserDetails user) {
        log.trace("Enter");
        return new ResponseEntity<>(this.watchlistService.getStockWatchlistByUserId(user.getUsername()), HttpStatus.OK);
    }

    @DeleteMapping("/crypto/{id}")
    public ResponseEntity<HttpStatus> deleteCryptoWatch(@AuthenticationPrincipal UserDetails user, @PathVariable Long id) {
        log.trace("Enter");
        this.watchlistService.deleteCryptoWatchById(user.getUsername(), id);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @DeleteMapping("/stock/{id}")
    public ResponseEntity<HttpStatus> deleteStockWatch(@AuthenticationPrincipal UserDetails user, @PathVariable Long id) {
        log.trace("Enter");
        this.watchlistService.deleteStockWatchById(user.getUsername(), id);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @DeleteMapping("/forex/{id}")
    public ResponseEntity<HttpStatus> deleteForexWatch(@AuthenticationPrincipal UserDetails user, @PathVariable Long id) {
        log.trace("Enter");
        this.watchlistService.deleteForexWatchById(user.getUsername(), id);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/stock")
    public ResponseEntity<StockWatchDTO> createStockWatch(@AuthenticationPrincipal UserDetails user, @RequestBody Stock wStock) {
        log.trace("Enter");
        return new ResponseEntity<>(this.watchlistService.createNewStockWatch(user.getUsername(), wStock), HttpStatus.OK);
    }

    @PostMapping("/crypto/{coinId}")
    public ResponseEntity<CryptoWatchDTO> createCryptoWatch(@AuthenticationPrincipal UserDetails user, @PathVariable String coinId) {
        log.trace("Enter");
        return new ResponseEntity<>(this.watchlistService.createNewCryptoWatch(user.getUsername(), coinId), HttpStatus.OK);
    }
}
