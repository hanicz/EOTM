package eye.on.the.money.controller;

import eye.on.the.money.dto.in.RSUGrantEditDTO;
import eye.on.the.money.dto.out.RSUGrantDTO;
import eye.on.the.money.dto.out.RSUGrantReportDTO;
import eye.on.the.money.dto.out.TaxBreakdownDTO;
import eye.on.the.money.model.stock.VestingFrequency;
import eye.on.the.money.service.stock.RSUGrantService;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class RSUGrantControllerTest {

    private static final Long USER_ID = 1L;

    @Mock
    private RSUGrantService rsuGrantService;

    @InjectMocks
    private RSUGrantController rsuGrantController;

    private RSUGrantEditDTO editDTO() {
        return new RSUGrantEditDTO("ACME", "US", null, LocalDate.of(2024, 3, 1), 400, 4,
                VestingFrequency.ANNUAL, null);
    }

    private RSUGrantDTO grantDTO(Long id) {
        return RSUGrantDTO.builder().id(id).shortName("ACME").exchange("US")
                .grantDate(LocalDate.of(2024, 3, 1)).quantity(400).vestingYears(4)
                .vestingFrequency(VestingFrequency.ANNUAL).vests(List.of())
                .totalAmountInHuf(new BigDecimal("4000")).totalTax(TaxBreakdownDTO.zero())
                .totalNetInHuf(new BigDecimal("4000")).build();
    }

    @Test
    void getGrants_returnsTheReportForTheSignedInUser() {
        RSUGrantReportDTO report = RSUGrantReportDTO.builder().items(List.of(this.grantDTO(1L)))
                .totalAmountInHuf(new BigDecimal("4000")).totalTax(TaxBreakdownDTO.zero())
                .totalNetInHuf(new BigDecimal("4000")).build();
        when(this.rsuGrantService.getGrants(USER_ID)).thenReturn(report);

        ResponseEntity<RSUGrantReportDTO> response = this.rsuGrantController.getGrants(USER_ID);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(report, response.getBody());
    }

    @Test
    void createGrant_handsTheBodyToTheService() {
        RSUGrantEditDTO editDTO = this.editDTO();
        when(this.rsuGrantService.createGrant(USER_ID, editDTO)).thenReturn(this.grantDTO(7L));

        ResponseEntity<RSUGrantDTO> response = this.rsuGrantController.createGrant(USER_ID, editDTO);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(7L, response.getBody().getId());
    }

    @Test
    void updateGrant_passesThePathIdThrough() {
        RSUGrantEditDTO editDTO = this.editDTO();
        when(this.rsuGrantService.updateGrant(USER_ID, 7L, editDTO)).thenReturn(this.grantDTO(7L));

        ResponseEntity<RSUGrantDTO> response = this.rsuGrantController.updateGrant(USER_ID, 7L, editDTO);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(this.rsuGrantService).updateGrant(eq(USER_ID), eq(7L), any(RSUGrantEditDTO.class));
    }

    @Test
    void deleteByIds_scopesTheDeleteToTheSignedInUser() {
        ResponseEntity<Void> response = this.rsuGrantController.deleteByIds(USER_ID, List.of(1L, 2L));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(this.rsuGrantService).deleteGrantsByIds(USER_ID, List.of(1L, 2L));
    }

    @Test
    void getCSV_marksTheResponseAsADownload() throws Exception {
        HttpServletResponse servletResponse = new MockHttpServletResponse();

        this.rsuGrantController.getCSV(USER_ID, servletResponse);

        assertEquals("attachment; filename=\"rsu-grants.csv\"",
                servletResponse.getHeader("Content-Disposition"));
        verify(this.rsuGrantService).getCSV(eq(USER_ID), any());
    }
}
