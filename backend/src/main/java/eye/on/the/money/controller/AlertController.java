package eye.on.the.money.controller;

import eye.on.the.money.dto.out.StockAlertDTO;
import eye.on.the.money.service.AlertService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("alert")
@Slf4j
public class AlertController {

    private final AlertService alertService;

    @Autowired
    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

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
