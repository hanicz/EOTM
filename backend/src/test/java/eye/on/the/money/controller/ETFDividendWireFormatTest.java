package eye.on.the.money.controller;

import eye.on.the.money.EotmApplication;
import eye.on.the.money.dto.out.ETFDividendDTO;
import eye.on.the.money.service.etf.ETFDividendService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@SpringBootTest(classes = EotmApplication.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
class ETFDividendWireFormatTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ETFDividendService etfDividendService;

    @Test
    void listCarriesTheRowId() throws Exception {
        when(this.etfDividendService.getDividends(any())).thenReturn(List.of(
                ETFDividendDTO.builder()
                        .id(42L)
                        .amount(105.7)
                        .dividendDate(LocalDate.parse("2021-07-03"))
                        .shortName("VWRL")
                        .name("Vang FTSE AllW-D")
                        .currencyId("EUR")
                        .exchange("AS")
                        .build()));

        MvcResult result = this.mockMvc.perform(get("/api/v1/etfdividend")).andReturn();
        String json = result.getResponse().getContentAsString();

        assertTrue(json.contains("\"id\":42"), "row id missing from the wire format: " + json);
    }

    @Test
    void deleteAcceptsTheTrailingCommaTheUiSends() throws Exception {
        MvcResult result = this.mockMvc.perform(delete("/api/v1/etfdividend").param("ids", "1,")).andReturn();
        assertTrue(result.getResponse().getStatus() == 200,
                "trailing-comma ids rejected with " + result.getResponse().getStatus()
                        + ": " + result.getResponse().getContentAsString());
    }
}
