package eye.on.the.money.controller;

import eye.on.the.money.dto.in.SalaryEditDTO;
import eye.on.the.money.dto.out.SalaryDTO;
import eye.on.the.money.model.salary.SalaryBasis;
import eye.on.the.money.service.salary.SalaryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class SalaryControllerTest {

    private static final Long USER_ID = 1L;
    private static final LocalDate FROM = LocalDate.of(2024, 6, 1);

    @Mock
    private SalaryService salaryService;

    @InjectMocks
    private SalaryController salaryController;

    private SalaryDTO dto(Long id) {
        return SalaryDTO.builder().id(id).amount(new BigDecimal("600000")).basis(SalaryBasis.MONTHLY)
                .currencyId("HUF").validFrom(FROM).dependents(0)
                .grossMonthly(new BigDecimal("600000")).netMonthly(new BigDecimal("399000")).build();
    }

    private SalaryEditDTO editDTO() {
        return new SalaryEditDTO(new BigDecimal("600000"), SalaryBasis.MONTHLY, "HUF", FROM, null, 0, null);
    }

    @Test
    void getAllSalaries_returnsTheUsersSalaries() {
        when(this.salaryService.getSalaries(USER_ID)).thenReturn(List.of(this.dto(5L)));

        ResponseEntity<List<SalaryDTO>> response = this.salaryController.getAllSalaries(USER_ID);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals(5L, response.getBody().get(0).getId());
    }

    @Test
    void createSalary_returnsTheCreatedSalary() {
        SalaryEditDTO editDTO = this.editDTO();
        when(this.salaryService.createSalary(USER_ID, editDTO)).thenReturn(this.dto(5L));

        ResponseEntity<SalaryDTO> response = this.salaryController.createSalary(USER_ID, editDTO);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(0, new BigDecimal("399000").compareTo(response.getBody().getNetMonthly()));
    }

    @Test
    void updateSalary_returnsTheUpdatedSalary() {
        SalaryEditDTO editDTO = this.editDTO();
        when(this.salaryService.updateSalary(USER_ID, 5L, editDTO)).thenReturn(this.dto(5L));

        ResponseEntity<SalaryDTO> response = this.salaryController.updateSalary(USER_ID, 5L, editDTO);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(5L, response.getBody().getId());
    }

    @Test
    void deleteByIds_passesTheIdsToTheService() {
        ResponseEntity<Void> response = this.salaryController.deleteByIds(USER_ID, List.of(1L, 2L));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(this.salaryService).deleteSalariesByIds(USER_ID, List.of(1L, 2L));
    }
}
