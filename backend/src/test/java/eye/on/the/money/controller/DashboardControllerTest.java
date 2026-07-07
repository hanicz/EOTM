package eye.on.the.money.controller;

import eye.on.the.money.dto.out.DashboardRatesDTO;
import eye.on.the.money.service.shared.DashboardService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class DashboardControllerTest {

    @Mock
    private DashboardService dashboardService;

    @InjectMocks
    private DashboardController dashboardController;

    @Test
    void getConversionRates_returnsRates() {
        DashboardRatesDTO dto = DashboardRatesDTO.builder()
                .rates(Map.of("USD", 1.08, "GBP", 0.86))
                .build();
        when(this.dashboardService.getConversionRates(List.of("USD", "GBP"))).thenReturn(dto);

        Assertions.assertEquals(dto, this.dashboardController.getConversionRates(List.of("USD", "GBP")).getBody());
    }

    @Test
    void getConversionRates_handlesNullCurrencies() {
        DashboardRatesDTO dto = DashboardRatesDTO.builder().rates(Map.of()).build();
        when(this.dashboardService.getConversionRates(List.of())).thenReturn(dto);

        Assertions.assertEquals(dto, this.dashboardController.getConversionRates(null).getBody());
    }

    @Test
    void getConversionRates_handlesEmptyList() {
        DashboardRatesDTO dto = DashboardRatesDTO.builder().rates(Map.of()).build();
        when(this.dashboardService.getConversionRates(List.of())).thenReturn(dto);

        Assertions.assertEquals(dto, this.dashboardController.getConversionRates(List.of()).getBody());
    }
}
