package eye.on.the.money.controller;

import eye.on.the.money.dto.out.StockAlertDTO;
import eye.on.the.money.model.User;
import eye.on.the.money.service.AlertService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("alert")
@Slf4j
public class AlertController {

    @Autowired
    private AlertService alertService;

    @GetMapping()
    public ResponseEntity<List<StockAlertDTO>> getAlerts(@AuthenticationPrincipal User user) {
        log.trace("Enter");
        return new ResponseEntity<List<StockAlertDTO>>(this.alertService.getAllStockAlerts(user.getId()), HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<HttpStatus> deleteAlert(@AuthenticationPrincipal User user, @PathVariable Long id) {
        log.trace("Enter");
        var isDeleted = this.alertService.deleteStockAlert(user.getId(), id);
        return new ResponseEntity<>(isDeleted ? HttpStatus.OK : HttpStatus.NOT_FOUND);
    }

    @PostMapping()
    public ResponseEntity<StockAlertDTO> createStockAlert(@AuthenticationPrincipal User user, @RequestBody StockAlertDTO stockAlertDTO) {
        log.trace("Enter");
        return new ResponseEntity<StockAlertDTO>(this.alertService.createNewStockAlert(user, stockAlertDTO), HttpStatus.OK);
    }
}
