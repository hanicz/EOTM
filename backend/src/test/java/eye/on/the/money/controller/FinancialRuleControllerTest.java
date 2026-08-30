package eye.on.the.money.controller;

import eye.on.the.money.dto.in.BankExclusionRuleEditDTO;
import eye.on.the.money.dto.out.BankExclusionRuleDTO;
import eye.on.the.money.model.financial.AccountSide;
import eye.on.the.money.service.financial.BankExclusionRuleService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class FinancialRuleControllerTest {

    private static final Long USER_ID = 1L;
    private static final String ACCOUNT = "12001008-00000000-00000001";

    @Mock
    private BankExclusionRuleService bankExclusionRuleService;

    @InjectMocks
    private FinancialRuleController financialRuleController;

    private BankExclusionRuleDTO dto(Long id) {
        return BankExclusionRuleDTO.builder().id(id).accountNumber(ACCOUNT)
                .side(AccountSide.PARTNER_ACCOUNT).active(true).build();
    }

    @Test
    void getAllRules_returnsTheUsersRules() {
        when(this.bankExclusionRuleService.getRules(USER_ID)).thenReturn(List.of(this.dto(5L)));

        ResponseEntity<List<BankExclusionRuleDTO>> response = this.financialRuleController.getAllRules(USER_ID);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals(5L, response.getBody().get(0).getId());
    }

    @Test
    void createRule_returnsTheCreatedRule() {
        BankExclusionRuleEditDTO editDTO = new BankExclusionRuleEditDTO(null, ACCOUNT, AccountSide.PARTNER_ACCOUNT, true);
        when(this.bankExclusionRuleService.createRule(USER_ID, editDTO)).thenReturn(this.dto(5L));

        ResponseEntity<BankExclusionRuleDTO> response = this.financialRuleController.createRule(USER_ID, editDTO);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(ACCOUNT, response.getBody().getAccountNumber());
    }

    @Test
    void updateRule_returnsTheUpdatedRule() {
        BankExclusionRuleEditDTO editDTO = new BankExclusionRuleEditDTO(null, ACCOUNT, AccountSide.ANY, false);
        when(this.bankExclusionRuleService.updateRule(USER_ID, 5L, editDTO)).thenReturn(this.dto(5L));

        ResponseEntity<BankExclusionRuleDTO> response = this.financialRuleController.updateRule(USER_ID, 5L, editDTO);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(5L, response.getBody().getId());
    }

    @Test
    void deleteByIds_passesTheIdsToTheService() {
        ResponseEntity<Void> response = this.financialRuleController.deleteByIds(USER_ID, List.of(1L, 2L));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(this.bankExclusionRuleService).deleteRulesByIds(USER_ID, List.of(1L, 2L));
    }
}
