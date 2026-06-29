package eye.on.the.money.service.shared;

import com.fasterxml.jackson.databind.ObjectMapper;
import eye.on.the.money.dto.out.DashboardRatesDTO;
import eye.on.the.money.service.api.EODAPIService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DashboardServiceTest {

    private EODAPIService eodAPIService;
    private DashboardService dashboardService;
    private final ObjectMapper om = new ObjectMapper();

    @BeforeEach
    void setUp() {
        this.eodAPIService = mock(EODAPIService.class);
        this.dashboardService = new DashboardService(this.eodAPIService);
    }

    @Test
    void getConversionRates_parsesCodeWithForexSuffix() throws Exception {
        when(this.eodAPIService.getLiveForexValue(anyString())).thenReturn(
                this.om.readTree("[{\"code\":\"EURUSD.FOREX\",\"close\":1.08}]"));

        DashboardRatesDTO result = this.dashboardService.getConversionRates(List.of("USD"));

        assertEquals(1.08, result.getRates().get("USD"));
    }

    @Test
    void getConversionRates_parsesCodeWithoutForexSuffix() throws Exception {
        when(this.eodAPIService.getLiveForexValue(anyString())).thenReturn(
                this.om.readTree("[{\"code\":\"EURHUF\",\"close\":405.2}]"));

        DashboardRatesDTO result = this.dashboardService.getConversionRates(List.of("HUF"));

        assertEquals(405.2, result.getRates().get("HUF"));
    }

    @Test
    void getConversionRates_handlesMultipleCurrencies() throws Exception {
        when(this.eodAPIService.getLiveForexValue(anyString())).thenReturn(
                this.om.readTree("[{\"code\":\"EURUSD.FOREX\",\"close\":1.08},{\"code\":\"EURHUF.FOREX\",\"close\":405.2}]"));

        DashboardRatesDTO result = this.dashboardService.getConversionRates(List.of("USD", "HUF"));

        assertEquals(1.08, result.getRates().get("USD"));
        assertEquals(405.2, result.getRates().get("HUF"));
    }

    @Test
    void getConversionRates_excludesBaseCurrencyFromLookup() {
        DashboardRatesDTO result = this.dashboardService.getConversionRates(List.of("EUR"));

        assertTrue(result.getRates().isEmpty());
    }

    @Test
    void getConversionRates_returnsEmptyMapWhenNoCurrenciesRequested() {
        DashboardRatesDTO result = this.dashboardService.getConversionRates(List.of());

        assertTrue(result.getRates().isEmpty());
    }
}
