package eye.on.the.money.controller;

import eye.on.the.money.dto.out.PensionDTO;
import eye.on.the.money.service.pension.PensionService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class PensionControllerTest {

    @Mock
    private PensionService pensionService;

    @InjectMocks
    private PensionController pensionController;

    @Test
    void getPension() {
        PensionDTO pension = PensionDTO.builder()
                .totalContribution(1_500_000.0).currentValue(1_800_000.0).currency("HUF").build();
        when(this.pensionService.getPension(anyLong())).thenReturn(pension);

        Assertions.assertEquals(pension, this.pensionController.getPension(1L).getBody());
        Assertions.assertEquals(HttpStatus.OK, this.pensionController.getPension(1L).getStatusCode());
    }

    @Test
    void updatePension() {
        PensionDTO pension = PensionDTO.builder()
                .totalContribution(1_650_000.0).currentValue(2_050_000.0).currency("HUF").build();
        when(this.pensionService.updatePension(anyLong(), any(PensionDTO.class))).thenReturn(pension);

        Assertions.assertEquals(pension, this.pensionController.updatePension(1L, pension).getBody());
        Assertions.assertEquals(HttpStatus.OK,
                this.pensionController.updatePension(1L, pension).getStatusCode());
    }
}
