package eye.on.the.money.controller;

import eye.on.the.money.dto.out.NetWorthDTO;
import eye.on.the.money.service.shared.NetWorthService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class NetWorthControllerTest {

    @Mock
    private NetWorthService netWorthService;

    @InjectMocks
    private NetWorthController netWorthController;

    @Test
    void getNetWorth_readsThroughTheCacheByDefault() {
        NetWorthDTO dto = this.netWorth();
        when(this.netWorthService.getNetWorth(1L, "EUR", false)).thenReturn(dto);

        Assertions.assertEquals(dto, this.netWorthController.getNetWorth(1L, "EUR", false).getBody());
    }

    @Test
    void getNetWorth_refreshBypassesTheCache() {
        NetWorthDTO dto = this.netWorth();
        when(this.netWorthService.getNetWorth(1L, "EUR", true)).thenReturn(dto);

        Assertions.assertEquals(dto, this.netWorthController.getNetWorth(1L, "EUR", true).getBody());
    }

    @Test
    void getNetWorth_acceptsAMissingCurrency() {
        NetWorthDTO dto = this.netWorth();
        when(this.netWorthService.getNetWorth(1L, null, false)).thenReturn(dto);

        Assertions.assertEquals(dto, this.netWorthController.getNetWorth(1L, null, false).getBody());
    }

    private NetWorthDTO netWorth() {
        return NetWorthDTO.builder()
                .currency("EUR")
                .totalSpent(BigDecimal.valueOf(100))
                .totalWorth(BigDecimal.valueOf(120))
                .totalChangePct(BigDecimal.valueOf(20))
                .assets(List.of())
                .availableCurrencies(List.of("EUR"))
                .unconvertedCurrencies(List.of())
                .build();
    }
}
