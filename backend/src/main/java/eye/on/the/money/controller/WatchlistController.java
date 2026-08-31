package eye.on.the.money.controller;

import eye.on.the.money.dto.in.WatchGroupEditDTO;
import eye.on.the.money.dto.out.CryptoWatchDTO;
import eye.on.the.money.dto.out.ForexWatchDTO;
import eye.on.the.money.dto.out.StockWatchDTO;
import eye.on.the.money.dto.out.WatchGroupDTO;
import eye.on.the.money.model.stock.Stock;
import eye.on.the.money.service.shared.WatchListService;
import eye.on.the.money.service.watchlist.WatchGroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import eye.on.the.money.security.CurrentUserId;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("api/v1/watchlist")
@Slf4j
@Validated
@RequiredArgsConstructor
public class WatchlistController {

    private final WatchListService watchlistService;
    private final WatchGroupService watchGroupService;

    @GetMapping("/crypto/{currency}")
    public ResponseEntity<List<CryptoWatchDTO>> getCryptoWatchList(@CurrentUserId Long userId, @PathVariable String currency) {
        List<CryptoWatchDTO> cryptoWatchList = this.watchlistService.getCryptoWatchlistByUserId(userId, currency);
        cryptoWatchList.sort(Collections.reverseOrder());
        return ResponseEntity.ok(cryptoWatchList);
    }

    @GetMapping("/forex")
    public ResponseEntity<List<ForexWatchDTO>> getForexWatchList(@CurrentUserId Long userId) {
        return ResponseEntity.ok(this.watchlistService.getForexWatchlistByUserId(userId));
    }

    @GetMapping("/stock")
    public ResponseEntity<List<StockWatchDTO>> getStockWatchList(@CurrentUserId Long userId) {
        return ResponseEntity.ok(this.watchlistService.getStockWatchlistByUserId(userId));
    }

    @DeleteMapping("/crypto/{id}")
    public ResponseEntity<Void> deleteCryptoWatch(@CurrentUserId Long userId, @PathVariable Long id) {
        this.watchlistService.deleteCryptoWatchById(userId, id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/stock/{id}")
    public ResponseEntity<Void> deleteStockWatch(@CurrentUserId Long userId, @PathVariable Long id) {
        this.watchlistService.deleteStockWatchById(userId, id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/forex/{id}")
    public ResponseEntity<Void> deleteForexWatch(@CurrentUserId Long userId, @PathVariable Long id) {
        this.watchlistService.deleteForexWatchById(userId, id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/stock")
    public ResponseEntity<StockWatchDTO> createStockWatch(@CurrentUserId Long userId, @RequestBody Stock wStock,
                                                         @RequestParam(required = false) Long groupId) {
        return ResponseEntity.ok(this.watchlistService.createNewStockWatch(userId, wStock, groupId));
    }

    @PutMapping("/stock/{id}/group")
    public ResponseEntity<StockWatchDTO> setStockWatchGroup(@CurrentUserId Long userId, @PathVariable Long id,
                                                            @RequestParam(required = false) Long groupId) {
        return ResponseEntity.ok(this.watchlistService.setStockWatchGroup(userId, id, groupId));
    }

    @PostMapping("/crypto/{coinId}")
    public ResponseEntity<CryptoWatchDTO> createCryptoWatch(@CurrentUserId Long userId, @PathVariable String coinId) {
        return ResponseEntity.ok(this.watchlistService.createNewCryptoWatch(userId, coinId));
    }

    @PostMapping("/forex/{from}/{to}")
    public ResponseEntity<ForexWatchDTO> createForexWatch(@CurrentUserId Long userId,
                                                          @PathVariable String from, @PathVariable String to) {
        return ResponseEntity.ok(this.watchlistService.createNewForexWatch(userId, from, to));
    }

    @GetMapping("/group")
    public ResponseEntity<List<WatchGroupDTO>> getGroups(@CurrentUserId Long userId) {
        return ResponseEntity.ok(this.watchGroupService.getGroups(userId));
    }

    @PostMapping("/group")
    public ResponseEntity<WatchGroupDTO> createGroup(@CurrentUserId Long userId,
                                                     @RequestBody @Valid WatchGroupEditDTO editDTO) {
        return ResponseEntity.ok(this.watchGroupService.createGroup(userId, editDTO));
    }

    @PutMapping("/group/{id}")
    public ResponseEntity<WatchGroupDTO> updateGroup(@CurrentUserId Long userId, @PathVariable Long id,
                                                     @RequestBody @Valid WatchGroupEditDTO editDTO) {
        return ResponseEntity.ok(this.watchGroupService.updateGroup(userId, id, editDTO));
    }

    @DeleteMapping("/group/{id}")
    public ResponseEntity<Void> deleteGroup(@CurrentUserId Long userId, @PathVariable Long id) {
        boolean isDeleted = this.watchGroupService.deleteGroup(userId, id);
        return ResponseEntity.status(isDeleted ? HttpStatus.OK : HttpStatus.NOT_FOUND).build();
    }
}
