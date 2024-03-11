package eye.on.the.money.controller;

import eye.on.the.money.dto.out.StockAlertDTO;
import eye.on.the.money.service.shared.AlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/v1/alert")
@Slf4j
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;

    @GetMapping("stock")
    public ResponseEntity<List<StockAlertDTO>> getStockAlerts(@AuthenticationPrincipal UserDetails user) {
        return new ResponseEntity<>(this.alertService.getAllStockAlerts(user.getUsername()), HttpStatus.OK);
    }

    @GetMapping("crypto")
    public ResponseEntity<List<CryptoAlertDTO>> getCryptoAlerts(@AuthenticationPrincipal UserDetails user) {
        return new ResponseEntity<>(this.alertService.getAllCryptoAlerts(user.getUsername()), HttpStatus.OK);
    }

    @DeleteMapping("crypto/{id}")
    public ResponseEntity<HttpStatus> deleteCryptoAlert(@AuthenticationPrincipal UserDetails user, @PathVariable Long id) {
        var isDeleted = this.alertService.deleteCryptoAlert(user.getUsername(), id);
        return new ResponseEntity<>(isDeleted ? HttpStatus.OK : HttpStatus.NOT_FOUND);
    }

    @DeleteMapping("stock/{id}")
    public ResponseEntity<HttpStatus> deleteStockAlert(@AuthenticationPrincipal UserDetails user, @PathVariable Long id) {
        var isDeleted = this.alertService.deleteStockAlert(user.getUsername(), id);
        return new ResponseEntity<>(isDeleted ? HttpStatus.OK : HttpStatus.NOT_FOUND);
    }

    @PostMapping("stock")
    public ResponseEntity<StockAlertDTO> createStockAlert(@AuthenticationPrincipal UserDetails user, @RequestBody StockAlertDTO stockAlertDTO) {
        log.trace("Enter");
        return new ResponseEntity<>(this.alertService.createNewStockAlert(user, stockAlertDTO), HttpStatus.OK);
    }

    @PostMapping("crypto")
    public ResponseEntity<CryptoAlertDTO> createCryptoAlert(@AuthenticationPrincipal UserDetails user, @RequestBody CryptoAlertDTO cryptoAlertDTO) {
        log.trace("Enter");
        return new ResponseEntity<>(this.alertService.createNewCryptoAlert(user, cryptoAlertDTO), HttpStatus.OK);
    }
}
