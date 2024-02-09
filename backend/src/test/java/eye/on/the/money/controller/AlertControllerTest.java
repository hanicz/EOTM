package eye.on.the.money.controller;

import eye.on.the.money.dto.out.StockAlertDTO;
import eye.on.the.money.model.User;
import eye.on.the.money.service.shared.AlertService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
class AlertControllerTest {

    @Mock
    private AlertService alertService;

    @InjectMocks
    private AlertController alertController;

    @Test
    public void getAlerts() {
        List<StockAlertDTO> alerts = new ArrayList<>();
        alerts.add(StockAlertDTO.builder().name("n1").shortName("sn1").valuePoint(0.1).type("t1").exchange("e1").id(1L).build());
        alerts.add(StockAlertDTO.builder().name("n2").shortName("sn2").valuePoint(0.2).type("t2").exchange("e2").id(1L).build());
        alerts.add(StockAlertDTO.builder().name("n3").shortName("sn3").valuePoint(0.3).type("t3").exchange("e3").id(1L).build());

        when(this.alertService.getAllStockAlerts(anyString())).thenReturn(alerts);

        ResponseEntity<List<StockAlertDTO>> result = this.alertController.getAlerts(User.builder().id(1L).email("email").build());

        Assertions.assertIterableEquals(result.getBody(), alerts);
    }

    @Test
    public void deleteAlert() {
        when(this.alertService.deleteStockAlert(anyString(), anyLong())).thenReturn(true);

        ResponseEntity<HttpStatus> result = this.alertController.deleteStockAlert(User.builder().id(1L).email("email").build(), 1L);

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    public void deleteAlert404() {
        when(this.alertService.deleteStockAlert(anyString(), anyLong())).thenReturn(false);

        ResponseEntity<HttpStatus> result = this.alertController.deleteStockAlert(User.builder().id(1L).build(), 1L);

        assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
    }

    @Test
    public void createStockAlert() {
        StockAlertDTO saDTO = StockAlertDTO.builder().name("n1").shortName("sn1").valuePoint(0.1).type("t1").exchange("e1").id(1L).build();
        User user = User.builder().id(1L).build();
        when(this.alertService.createNewStockAlert(user, saDTO)).thenReturn(saDTO);

        ResponseEntity<StockAlertDTO> result = this.alertController.createStockAlert(user, saDTO);

        Assertions.assertAll("Assert response",
                () -> assertEquals(saDTO, result.getBody()),
                () -> assertEquals(HttpStatus.OK, result.getStatusCode()));
    }
}