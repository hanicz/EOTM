package eye.on.the.money.controller;

import eye.on.the.money.dto.out.CryptoWatchDTO;
import eye.on.the.money.dto.out.ForexWatchDTO;
import eye.on.the.money.dto.out.StockWatchDTO;
import eye.on.the.money.model.stock.Stock;
import eye.on.the.money.service.shared.WatchListService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import eye.on.the.money.security.CurrentUserEmail;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("api/v1/watchlist")
@Slf4j
@RequiredArgsConstructor
public class WatchlistController {

    private final WatchListService watchlistService;

    @GetMapping("/crypto/{currency}")
    public ResponseEntity<List<CryptoWatchDTO>> getCryptoWatchList(@CurrentUserEmail String userEmail, @PathVariable String currency) {
        List<CryptoWatchDTO> cryptoWatchList = this.watchlistService.getCryptoWatchlistByUserId(userEmail, currency);
        cryptoWatchList.sort(Collections.reverseOrder());
        return ResponseEntity.ok(cryptoWatchList);
    }

    @GetMapping("/forex")
    public ResponseEntity<List<ForexWatchDTO>> getForexWatchList(@CurrentUserEmail String userEmail) {
        return ResponseEntity.ok(this.watchlistService.getForexWatchlistByUserId(userEmail));
    }

    @GetMapping("/stock")
    public ResponseEntity<List<StockWatchDTO>> getStockWatchList(@CurrentUserEmail String userEmail) {
        return ResponseEntity.ok(this.watchlistService.getStockWatchlistByUserId(userEmail));
    }

    @DeleteMapping("/crypto/{id}")
    public ResponseEntity<Void> deleteCryptoWatch(@CurrentUserEmail String userEmail, @PathVariable Long id) {
        this.watchlistService.deleteCryptoWatchById(userEmail, id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/stock/{id}")
    public ResponseEntity<Void> deleteStockWatch(@CurrentUserEmail String userEmail, @PathVariable Long id) {
        this.watchlistService.deleteStockWatchById(userEmail, id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/forex/{id}")
    public ResponseEntity<Void> deleteForexWatch(@CurrentUserEmail String userEmail, @PathVariable Long id) {
        this.watchlistService.deleteForexWatchById(userEmail, id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/stock")
    public ResponseEntity<StockWatchDTO> createStockWatch(@CurrentUserEmail String userEmail, @RequestBody Stock wStock) {
        return ResponseEntity.ok(this.watchlistService.createNewStockWatch(userEmail, wStock));
    }

    @PostMapping("/crypto/{coinId}")
    public ResponseEntity<CryptoWatchDTO> createCryptoWatch(@CurrentUserEmail String userEmail, @PathVariable String coinId) {
        return ResponseEntity.ok(this.watchlistService.createNewCryptoWatch(userEmail, coinId));
    }

    @PostMapping("/forex/{from}/{to}")
    public ResponseEntity<ForexWatchDTO> createForexWatch(@CurrentUserEmail String userEmail,
                                                          @PathVariable String from, @PathVariable String to) {
        return ResponseEntity.ok(this.watchlistService.createNewForexWatch(userEmail, from, to));
    }
}
