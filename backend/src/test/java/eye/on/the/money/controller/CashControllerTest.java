package eye.on.the.money.controller;

import eye.on.the.money.dto.out.CashDTO;
import eye.on.the.money.service.cash.CashService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class CashControllerTest {

    private static final Long USER_ID = 42L;

    @Mock
    private CashService cashService;
    @InjectMocks
    private CashController cashController;

    @Test
    void getCash_returnsTheStoredBalance() {
        CashDTO cash = CashDTO.builder().amount(750000.0).currency("HUF").build();
        when(this.cashService.getCash(USER_ID)).thenReturn(cash);

        ResponseEntity<CashDTO> response = this.cashController.getCash(USER_ID);

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertEquals(cash, response.getBody());
    }

    @Test
    void updateCash_passesTheNewBalanceToTheService() {
        CashDTO request = CashDTO.builder().amount(900000.0).build();
        CashDTO saved = CashDTO.builder().amount(900000.0).currency("HUF").build();
        when(this.cashService.updateCash(USER_ID, request)).thenReturn(saved);

        ResponseEntity<CashDTO> response = this.cashController.updateCash(USER_ID, request);

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertEquals(saved, response.getBody());
        verify(this.cashService).updateCash(USER_ID, request);
    }
}
