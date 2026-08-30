package eye.on.the.money.service.financial;

import eye.on.the.money.dto.in.BankExclusionRuleEditDTO;
import eye.on.the.money.dto.out.BankExclusionRuleDTO;
import eye.on.the.money.exception.ValidationException;
import eye.on.the.money.model.User;
import eye.on.the.money.model.financial.AccountSide;
import eye.on.the.money.model.financial.BankExclusionRule;
import eye.on.the.money.repository.financial.BankExclusionRuleRepository;
import eye.on.the.money.service.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BankExclusionRuleServiceTest {

    private static final Long USER_ID = 1L;
    private static final String ACCOUNT = "12001008-00000000-00000001";
    private static final String NORMALIZED = "120010080000000000000001";

    @Mock
    private BankExclusionRuleRepository bankExclusionRuleRepository;
    @Mock
    private UserService userService;

    @InjectMocks
    private BankExclusionRuleService bankExclusionRuleService;

    private final User user = User.builder().id(USER_ID).email("test@email.com").build();

    @BeforeEach
    void setUp() {
        when(this.userService.getReference(USER_ID)).thenReturn(this.user);
        when(this.bankExclusionRuleRepository.findByUserIdAndNormalizedAccountAndSide(anyLong(), anyString(), any()))
                .thenReturn(Optional.empty());
        when(this.bankExclusionRuleRepository.saveAndFlush(any(BankExclusionRule.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private BankExclusionRule existingRule(Long id, AccountSide side) {
        return BankExclusionRule.builder().id(id).accountNumber(ACCOUNT).normalizedAccount(NORMALIZED)
                .side(side).active(true).user(this.user).build();
    }

    private BankExclusionRule captureSave() {
        ArgumentCaptor<BankExclusionRule> captor = ArgumentCaptor.forClass(BankExclusionRule.class);
        verify(this.bankExclusionRuleRepository).saveAndFlush(captor.capture());
        return captor.getValue();
    }

    @Test
    void createRule_storesTheNormalizedAccountAndKeepsTheTypedOne() {
        BankExclusionRuleDTO dto = this.bankExclusionRuleService.createRule(USER_ID,
                new BankExclusionRuleEditDTO(null, "  " + ACCOUNT + "  ", AccountSide.PARTNER_ACCOUNT, true));

        BankExclusionRule saved = this.captureSave();
        assertEquals(ACCOUNT, saved.getAccountNumber());
        assertEquals(NORMALIZED, saved.getNormalizedAccount());
        assertEquals(AccountSide.PARTNER_ACCOUNT, saved.getSide());
        assertTrue(saved.isActive());
        assertEquals(this.user, saved.getUser());
        assertEquals(ACCOUNT, dto.getAccountNumber());
    }

    @Test
    void createRule_keepsTheNameWithoutItsSurroundingSpaces() {
        BankExclusionRuleDTO dto = this.bankExclusionRuleService.createRule(USER_ID,
                new BankExclusionRuleEditDTO("  Rent  ", ACCOUNT, AccountSide.PARTNER_ACCOUNT, true));

        assertEquals("Rent", this.captureSave().getName());
        assertEquals("Rent", dto.getName());
    }

    @Test
    void createRule_storesABlankNameAsNull() {
        this.bankExclusionRuleService.createRule(USER_ID,
                new BankExclusionRuleEditDTO("   ", ACCOUNT, AccountSide.PARTNER_ACCOUNT, true));

        assertNull(this.captureSave().getName());
    }

    @Test
    void createRule_storesAMissingNameAsNull() {
        this.bankExclusionRuleService.createRule(USER_ID,
                new BankExclusionRuleEditDTO(null, ACCOUNT, AccountSide.PARTNER_ACCOUNT, true));

        assertNull(this.captureSave().getName());
    }

    @Test
    void createRule_rejectsAnAccountWithNoLettersOrDigits() {
        assertThrows(ValidationException.class, () -> this.bankExclusionRuleService.createRule(USER_ID,
                new BankExclusionRuleEditDTO(null, "---//---", AccountSide.ANY, true)));

        verify(this.bankExclusionRuleRepository, never()).saveAndFlush(any());
    }

    @Test
    void createRule_rejectsADuplicate() {
        when(this.bankExclusionRuleRepository.findByUserIdAndNormalizedAccountAndSide(
                USER_ID, NORMALIZED, AccountSide.PARTNER_ACCOUNT))
                .thenReturn(Optional.of(this.existingRule(9L, AccountSide.PARTNER_ACCOUNT)));

        assertThrows(ValidationException.class, () -> this.bankExclusionRuleService.createRule(USER_ID,
                new BankExclusionRuleEditDTO(null, ACCOUNT, AccountSide.PARTNER_ACCOUNT, true)));

        verify(this.bankExclusionRuleRepository, never()).saveAndFlush(any());
    }

    @Test
    void createRule_translatesADatabaseConstraintBreachIntoAValidationError() {
        when(this.bankExclusionRuleRepository.saveAndFlush(any(BankExclusionRule.class)))
                .thenThrow(new DataIntegrityViolationException("UK_BANK_EXCLUSION_RULE_ACCOUNT"));

        assertThrows(ValidationException.class, () -> this.bankExclusionRuleService.createRule(USER_ID,
                new BankExclusionRuleEditDTO(null, ACCOUNT, AccountSide.ANY, true)));
    }

    @Test
    void createRule_storesAnInactiveRuleWhenAskedTo() {
        this.bankExclusionRuleService.createRule(USER_ID,
                new BankExclusionRuleEditDTO(null, ACCOUNT, AccountSide.OWN_ACCOUNT, false));

        assertFalse(this.captureSave().isActive());
    }

    @Test
    void updateRule_savesTheNewValues() {
        when(this.bankExclusionRuleRepository.findByIdAndUserId(5L, USER_ID))
                .thenReturn(Optional.of(this.existingRule(5L, AccountSide.PARTNER_ACCOUNT)));

        this.bankExclusionRuleService.updateRule(USER_ID, 5L,
                new BankExclusionRuleEditDTO("Savings transfer", "1111-1111", AccountSide.OWN_ACCOUNT, false));

        BankExclusionRule saved = this.captureSave();
        assertEquals("Savings transfer", saved.getName());
        assertEquals("1111-1111", saved.getAccountNumber());
        assertEquals("11111111", saved.getNormalizedAccount());
        assertEquals(AccountSide.OWN_ACCOUNT, saved.getSide());
        assertFalse(saved.isActive());
    }

    @Test
    void updateRule_allowsARuleToKeepItsOwnAccountAndSide() {
        when(this.bankExclusionRuleRepository.findByIdAndUserId(5L, USER_ID))
                .thenReturn(Optional.of(this.existingRule(5L, AccountSide.PARTNER_ACCOUNT)));
        when(this.bankExclusionRuleRepository.findByUserIdAndNormalizedAccountAndSide(
                USER_ID, NORMALIZED, AccountSide.PARTNER_ACCOUNT))
                .thenReturn(Optional.of(this.existingRule(5L, AccountSide.PARTNER_ACCOUNT)));

        this.bankExclusionRuleService.updateRule(USER_ID, 5L,
                new BankExclusionRuleEditDTO(null, ACCOUNT, AccountSide.PARTNER_ACCOUNT, false));

        assertFalse(this.captureSave().isActive());
    }

    @Test
    void updateRule_rejectsAnAccountAndSideAnotherRuleAlreadyUses() {
        when(this.bankExclusionRuleRepository.findByIdAndUserId(5L, USER_ID))
                .thenReturn(Optional.of(this.existingRule(5L, AccountSide.PARTNER_ACCOUNT)));
        when(this.bankExclusionRuleRepository.findByUserIdAndNormalizedAccountAndSide(
                USER_ID, NORMALIZED, AccountSide.PARTNER_ACCOUNT))
                .thenReturn(Optional.of(this.existingRule(9L, AccountSide.PARTNER_ACCOUNT)));

        assertThrows(ValidationException.class, () -> this.bankExclusionRuleService.updateRule(USER_ID, 5L,
                new BankExclusionRuleEditDTO(null, ACCOUNT, AccountSide.PARTNER_ACCOUNT, true)));
    }

    @Test
    void updateRule_throwsWhenTheRuleBelongsToSomeoneElse() {
        when(this.bankExclusionRuleRepository.findByIdAndUserId(5L, USER_ID)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> this.bankExclusionRuleService.updateRule(USER_ID, 5L,
                new BankExclusionRuleEditDTO(null, ACCOUNT, AccountSide.ANY, true)));
    }

    @Test
    void getRules_mapsEveryFieldToTheDTO() {
        when(this.bankExclusionRuleRepository.findByUserIdOrderByAccountNumberAsc(USER_ID))
                .thenReturn(List.of(this.existingRule(5L, AccountSide.ANY)));

        List<BankExclusionRuleDTO> rules = this.bankExclusionRuleService.getRules(USER_ID);

        assertEquals(1, rules.size());
        assertEquals(5L, rules.get(0).getId());
        assertEquals(ACCOUNT, rules.get(0).getAccountNumber());
        assertEquals(AccountSide.ANY, rules.get(0).getSide());
        assertTrue(rules.get(0).isActive());
    }

    @Test
    void matcherFor_onlyLoadsActiveRules() {
        when(this.bankExclusionRuleRepository.findByUserIdAndActiveTrue(USER_ID))
                .thenReturn(List.of(this.existingRule(5L, AccountSide.PARTNER_ACCOUNT)));

        ExclusionRuleMatcher matcher = this.bankExclusionRuleService.matcherFor(USER_ID);

        verify(this.bankExclusionRuleRepository).findByUserIdAndActiveTrue(USER_ID);
        assertTrue(matcher.matches("111111112222222233333333", ACCOUNT));
    }

    @Test
    void deleteRulesByIds_scopesTheDeleteToTheUser() {
        this.bankExclusionRuleService.deleteRulesByIds(USER_ID, List.of(1L, 2L));

        verify(this.bankExclusionRuleRepository).deleteByUserIdAndIdIn(USER_ID, List.of(1L, 2L));
    }
}
