package eye.on.the.money.controller;

import eye.on.the.money.dto.in.CompensationCompareDTO;
import eye.on.the.money.dto.in.CompensationEditDTO;
import eye.on.the.money.dto.out.CompensationItemDTO;
import eye.on.the.money.dto.out.CompensationPackageDTO;
import eye.on.the.money.model.salary.*;
import eye.on.the.money.service.salary.CompensationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class CompensationControllerTest {

    private static final Long USER_ID = 1L;

    @Mock
    private CompensationService compensationService;

    @InjectMocks
    private CompensationController compensationController;

    private CompensationPackageDTO packageDTO(String totalNetMonthly) {
        return CompensationPackageDTO.builder().currencyId("HUF").basis(SalaryBasis.MONTHLY)
                .baseGrossMonthly(new BigDecimal("600000")).baseNetMonthly(new BigDecimal("399000"))
                .items(List.of()).totalNetMonthly(new BigDecimal(totalNetMonthly)).build();
    }

    private CompensationItemDTO itemDTO(Long id) {
        return CompensationItemDTO.builder().id(id).name("Annual bonus")
                .amountMode(CompensationAmountMode.MONTHLY_AMOUNT).monthlyAmount(new BigDecimal("100000"))
                .taxTreatment(CompensationTaxTreatment.TAXED_AS_SALARY).currencyId("HUF").build();
    }

    private CompensationEditDTO editDTO() {
        return new CompensationEditDTO("Annual bonus", CompensationAmountMode.MONTHLY_AMOUNT,
                new BigDecimal("100000"), null, CompensationTaxTreatment.TAXED_AS_SALARY, null);
    }

    @Test
    void getCurrentPackage_returnsTheUsersPackage() {
        when(this.compensationService.getCurrentPackage(USER_ID)).thenReturn(Optional.of(this.packageDTO("399000")));

        ResponseEntity<CompensationPackageDTO> response = this.compensationController.getCurrentPackage(USER_ID);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("HUF", response.getBody().getCurrencyId());
    }

    @Test
    void getCurrentPackage_returnsNoContentWithoutASalary() {
        when(this.compensationService.getCurrentPackage(USER_ID)).thenReturn(Optional.empty());

        ResponseEntity<CompensationPackageDTO> response = this.compensationController.getCurrentPackage(USER_ID);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    void comparePackage_returnsThePricedDraft() {
        CompensationCompareDTO compareDTO = new CompensationCompareDTO("The offer", new BigDecimal("9600000"),
                SalaryBasis.ANNUAL, "HUF", 0, List.of());
        when(this.compensationService.comparePackage(compareDTO)).thenReturn(this.packageDTO("532000"));

        ResponseEntity<CompensationPackageDTO> response = this.compensationController.comparePackage(compareDTO);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(0, new BigDecimal("532000").compareTo(response.getBody().getTotalNetMonthly()));
    }

    @Test
    void createItem_returnsTheSavedItem() {
        when(this.compensationService.createItem(USER_ID, this.editDTO())).thenReturn(this.itemDTO(11L));

        ResponseEntity<CompensationItemDTO> response = this.compensationController.createItem(USER_ID, this.editDTO());

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(11L, response.getBody().getId());
    }

    @Test
    void updateItem_passesTheIdOn() {
        when(this.compensationService.updateItem(USER_ID, 11L, this.editDTO())).thenReturn(this.itemDTO(11L));

        ResponseEntity<CompensationItemDTO> response =
                this.compensationController.updateItem(USER_ID, 11L, this.editDTO());

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(11L, response.getBody().getId());
    }

    @Test
    void deleteByIds_passesEveryIdOn() {
        ResponseEntity<Void> response = this.compensationController.deleteByIds(USER_ID, List.of(11L, 12L));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(this.compensationService).deleteItemsByIds(USER_ID, List.of(11L, 12L));
    }
}
