package eye.on.the.money.controller;

import eye.on.the.money.dto.out.CryptoAlertDTO;
import eye.on.the.money.dto.out.StockAlertDTO;
import eye.on.the.money.service.shared.AlertService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
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

        ResponseEntity<List<StockAlertDTO>> result = this.alertController.getStockAlerts("email");

        Assertions.assertIterableEquals(result.getBody(), alerts);
    }

    @Test
    public void deleteAlert() {
        when(this.alertService.deleteStockAlert(anyString(), anyLong())).thenReturn(true);

        ResponseEntity<Void> result = this.alertController.deleteStockAlert("email", 1L);

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    public void deleteAlert404() {
        when(this.alertService.deleteStockAlert(anyString(), anyLong())).thenReturn(false);

        ResponseEntity<Void> result = this.alertController.deleteStockAlert("user@email.com", 1L);

        assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
    }

    @Test
    public void createStockAlert() {
        StockAlertDTO saDTO = StockAlertDTO.builder().name("n1").shortName("sn1").valuePoint(0.1).type("t1").exchange("e1").build();
        when(this.alertService.createNewStockAlert("email", saDTO)).thenReturn(saDTO);

        ResponseEntity<StockAlertDTO> result = this.alertController.createStockAlert("email", saDTO);

        Assertions.assertAll("Assert response",
                () -> assertEquals(saDTO, result.getBody()),
                () -> assertEquals(HttpStatus.OK, result.getStatusCode()));
    }

    @Test
    public void getCryptoAlerts() {
        List<CryptoAlertDTO> alerts = new ArrayList<>();
        alerts.add(CryptoAlertDTO.builder().name("n1").symbol("s1").valuePoint(0.1).type("t1").id(1L).build());
        alerts.add(CryptoAlertDTO.builder().name("n2").symbol("s2").valuePoint(0.2).type("t2").id(1L).build());
        alerts.add(CryptoAlertDTO.builder().name("n3").symbol("s3").valuePoint(0.3).type("t3").id(1L).build());

        when(this.alertService.getAllCryptoAlerts(anyString())).thenReturn(alerts);

        ResponseEntity<List<CryptoAlertDTO>> result = this.alertController.getCryptoAlerts("email");

        Assertions.assertIterableEquals(result.getBody(), alerts);
    }

    @Test
    public void deleteCryptoAlert() {
        when(this.alertService.deleteCryptoAlert(anyString(), anyLong())).thenReturn(true);

        ResponseEntity<Void> result = this.alertController.deleteCryptoAlert("email", 1L);

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    public void deleteCryptoAlert404() {
        when(this.alertService.deleteCryptoAlert(anyString(), anyLong())).thenReturn(false);

        ResponseEntity<Void> result = this.alertController.deleteCryptoAlert("user@email.com", 1L);

        assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
    }

    @Test
    public void createCryptoAlert() {
        CryptoAlertDTO caDTO = CryptoAlertDTO.builder().name("n1").symbol("sn1").valuePoint(0.1).type("t1").build();
        when(this.alertService.createNewCryptoAlert("email", caDTO)).thenReturn(caDTO);

        ResponseEntity<CryptoAlertDTO> result = this.alertController.createCryptoAlert("email", caDTO);

        Assertions.assertAll("Assert response",
                () -> assertEquals(caDTO, result.getBody()),
                () -> assertEquals(HttpStatus.OK, result.getStatusCode()));
    }
}