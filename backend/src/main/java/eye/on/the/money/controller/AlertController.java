package eye.on.the.money.controller;

import eye.on.the.money.dto.out.CryptoAlertDTO;
import eye.on.the.money.dto.out.StockAlertDTO;
import eye.on.the.money.service.shared.AlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import eye.on.the.money.security.CurrentUserId;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/v1/alert")
@Slf4j
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;

    @GetMapping("stock")
    public ResponseEntity<List<StockAlertDTO>> getStockAlerts(@CurrentUserId Long userId) {
        return ResponseEntity.ok(this.alertService.getAllStockAlerts(userId));
    }

    @GetMapping("crypto")
    public ResponseEntity<List<CryptoAlertDTO>> getCryptoAlerts(@CurrentUserId Long userId) {
        return ResponseEntity.ok(this.alertService.getAllCryptoAlerts(userId));
    }

    @DeleteMapping("crypto/{id}")
    public ResponseEntity<Void> deleteCryptoAlert(@CurrentUserId Long userId, @PathVariable Long id) {
        var isDeleted = this.alertService.deleteCryptoAlert(userId, id);
        return ResponseEntity.status(isDeleted ? HttpStatus.OK : HttpStatus.NOT_FOUND).build();
    }

    @DeleteMapping("stock/{id}")
    public ResponseEntity<Void> deleteStockAlert(@CurrentUserId Long userId, @PathVariable Long id) {
        var isDeleted = this.alertService.deleteStockAlert(userId, id);
        return ResponseEntity.status(isDeleted ? HttpStatus.OK : HttpStatus.NOT_FOUND).build();
    }

    @PostMapping("stock")
    public ResponseEntity<StockAlertDTO> createStockAlert(@CurrentUserId Long userId, @RequestBody StockAlertDTO stockAlertDTO) {
        log.trace("Enter");
        return ResponseEntity.ok(this.alertService.createNewStockAlert(userId, stockAlertDTO));
    }

    @PostMapping("crypto")
    public ResponseEntity<CryptoAlertDTO> createCryptoAlert(@CurrentUserId Long userId, @RequestBody CryptoAlertDTO cryptoAlertDTO) {
        log.trace("Enter");
        return ResponseEntity.ok(this.alertService.createNewCryptoAlert(userId, cryptoAlertDTO));
    }
}
