package eye.on.the.money.controller;

import eye.on.the.money.dto.out.CryptoAlertDTO;
import eye.on.the.money.dto.out.StockAlertDTO;
import eye.on.the.money.service.shared.AlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import eye.on.the.money.security.CurrentUserEmail;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/v1/alert")
@Slf4j
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;

    @GetMapping("stock")
    public ResponseEntity<List<StockAlertDTO>> getStockAlerts(@CurrentUserEmail String userEmail) {
        return ResponseEntity.ok(this.alertService.getAllStockAlerts(userEmail));
    }

    @GetMapping("crypto")
    public ResponseEntity<List<CryptoAlertDTO>> getCryptoAlerts(@CurrentUserEmail String userEmail) {
        return ResponseEntity.ok(this.alertService.getAllCryptoAlerts(userEmail));
    }

    @DeleteMapping("crypto/{id}")
    public ResponseEntity<Void> deleteCryptoAlert(@CurrentUserEmail String userEmail, @PathVariable Long id) {
        var isDeleted = this.alertService.deleteCryptoAlert(userEmail, id);
        return ResponseEntity.status(isDeleted ? HttpStatus.OK : HttpStatus.NOT_FOUND).build();
    }

    @DeleteMapping("stock/{id}")
    public ResponseEntity<Void> deleteStockAlert(@CurrentUserEmail String userEmail, @PathVariable Long id) {
        var isDeleted = this.alertService.deleteStockAlert(userEmail, id);
        return ResponseEntity.status(isDeleted ? HttpStatus.OK : HttpStatus.NOT_FOUND).build();
    }

    @PostMapping("stock")
    public ResponseEntity<StockAlertDTO> createStockAlert(@CurrentUserEmail String userEmail, @RequestBody StockAlertDTO stockAlertDTO) {
        log.trace("Enter");
        return ResponseEntity.ok(this.alertService.createNewStockAlert(userEmail, stockAlertDTO));
    }

    @PostMapping("crypto")
    public ResponseEntity<CryptoAlertDTO> createCryptoAlert(@CurrentUserEmail String userEmail, @RequestBody CryptoAlertDTO cryptoAlertDTO) {
        log.trace("Enter");
        return ResponseEntity.ok(this.alertService.createNewCryptoAlert(userEmail, cryptoAlertDTO));
    }
}
