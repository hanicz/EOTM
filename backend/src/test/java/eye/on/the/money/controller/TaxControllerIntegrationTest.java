package eye.on.the.money.controller;

import eye.on.the.money.EotmApplication;
import eye.on.the.money.dto.out.RSUTaxDTO;
import eye.on.the.money.dto.out.TaxBreakdownDTO;
import eye.on.the.money.dto.out.TaxReportDTO;
import eye.on.the.money.service.shared.TaxService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = EotmApplication.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
class TaxControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TaxService taxService;

    /**
     * Guards the wire format: a LocalDate has to reach the UI as "2026-08-04", not as [2026,8,4], which
     * cannot be formatted and which makes two equal dates compare unequal.
     */
    @Test
    void serializesLocalDateAsIsoString() throws Exception {
        LocalDate date = LocalDate.of(2026, 8, 4);
        when(this.taxService.calculateTaxForRSUs(any())).thenReturn(TaxReportDTO.builder()
                .items(List.of(RSUTaxDTO.builder()
                        .shortName("AAPL").exchange("US").date(date).priceDate(date).rateDate(date)
                        .quantity(10).currency("USD").amountInHuf(new BigDecimal("1000"))
                        .tax(TaxBreakdownDTO.zero()).build()))
                .totalAmountInHuf(new BigDecimal("1000")).totalTax(TaxBreakdownDTO.zero()).build());

        MvcResult response = this.mockMvc.perform(post("/api/v1/tax/rsu")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"shortName\":\"AAPL\",\"date\":\"2026-08-04\",\"quantity\":10}]"))
                .andExpect(status().isOk())
                .andReturn();

        String json = response.getResponse().getContentAsString();
        assertTrue(json.contains("\"date\":\"2026-08-04\""), "date was not an ISO string: " + json);
        assertTrue(json.contains("\"priceDate\":\"2026-08-04\""), "priceDate was not an ISO string: " + json);
        assertTrue(json.contains("\"rateDate\":\"2026-08-04\""), "rateDate was not an ISO string: " + json);
    }
}
