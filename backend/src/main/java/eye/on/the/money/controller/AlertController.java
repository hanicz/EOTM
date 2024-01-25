package eye.on.the.money.controller;

import eye.on.the.money.dto.out.StockAlertDTO;
import eye.on.the.money.service.AlertService;
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

    @GetMapping()
    public ResponseEntity<List<StockAlertDTO>> getAlerts(@AuthenticationPrincipal UserDetails user) {
        log.trace("Enter");
        return new ResponseEntity<>(this.alertService.getAllStockAlerts(user.getUsername()), HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<HttpStatus> deleteStockAlert(@AuthenticationPrincipal UserDetails user, @PathVariable Long id) {
        log.trace("Enter");
        var isDeleted = this.alertService.deleteStockAlert(user.getUsername(), id);
        return new ResponseEntity<>(isDeleted ? HttpStatus.OK : HttpStatus.NOT_FOUND);
    }

    @PostMapping()
    public ResponseEntity<StockAlertDTO> createStockAlert(@AuthenticationPrincipal UserDetails user, @RequestBody StockAlertDTO stockAlertDTO) {
        log.trace("Enter");
        return new ResponseEntity<>(this.alertService.createNewStockAlert(user, stockAlertDTO), HttpStatus.OK);
    }
}
