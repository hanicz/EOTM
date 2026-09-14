package eye.on.the.money.service.financial;

import eye.on.the.money.dto.in.BankCategoryRuleEditDTO;
import eye.on.the.money.dto.out.BankCategoryRuleDTO;
import eye.on.the.money.dto.out.CategorizeResultDTO;
import eye.on.the.money.exception.ValidationException;
import eye.on.the.money.model.User;
import eye.on.the.money.model.financial.BankCategoryRule;
import eye.on.the.money.model.financial.BankTransaction;
import eye.on.the.money.model.financial.CategoryColor;
import eye.on.the.money.model.financial.SpendingCategory;
import eye.on.the.money.repository.financial.BankCategoryRuleRepository;
import eye.on.the.money.repository.financial.BankTransactionRepository;
import eye.on.the.money.repository.financial.SpendingCategoryRepository;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BankCategoryRuleServiceTest {

    private static final Long USER_ID = 1L;

    @Mock
    private BankCategoryRuleRepository bankCategoryRuleRepository;
    @Mock
    private SpendingCategoryRepository spendingCategoryRepository;
    @Mock
    private BankTransactionRepository bankTransactionRepository;
    @Mock
    private UserService userService;

    @InjectMocks
    private BankCategoryRuleService bankCategoryRuleService;

    private final User user = User.builder().id(USER_ID).email("test@email.com").build();
    private final SpendingCategory groceries = SpendingCategory.builder().id(3L).name("Groceries")
            .normalizedName("GROCERIES").color(CategoryColor.GREEN).build();
    private final SpendingCategory travel = SpendingCategory.builder().id(4L).name("Travel")
            .normalizedName("TRAVEL").color(CategoryColor.ORANGE).build();

    @BeforeEach
    void setUp() {
        when(this.userService.getReference(USER_ID)).thenReturn(this.user);
        when(this.spendingCategoryRepository.findByIdAndUserId(3L, USER_ID)).thenReturn(Optional.of(this.groceries));
        when(this.spendingCategoryRepository.findByIdAndUserId(4L, USER_ID)).thenReturn(Optional.of(this.travel));
        when(this.bankCategoryRuleRepository.saveAndFlush(any(BankCategoryRule.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(this.bankCategoryRuleRepository.findByUserIdAndNormalizedPattern(anyLong(), anyString()))
                .thenReturn(Optional.empty());
        when(this.bankCategoryRuleRepository.findByUserIdOrderByPriorityAscPatternAsc(USER_ID))
                .thenReturn(List.of());
    }

    private BankCategoryRule rule(String pattern, int priority, SpendingCategory category) {
        return BankCategoryRule.builder()
                .id((long) priority)
                .pattern(pattern)
                .normalizedPattern(CategoryRuleMatcher.normalize(pattern))
                .priority(priority)
                .active(true)
                .category(category)
                .build();
    }

    private BankTransaction transaction(Long id, String partnerName, SpendingCategory category, boolean locked) {
        return BankTransaction.builder()
                .id(id)
                .partnerName(partnerName)
                .category(category)
                .categoryLocked(locked)
                .build();
    }

    @Test
    void createRule_trimsThePatternAndStoresItsNormalisedForm() {
        BankCategoryRuleDTO created = this.bankCategoryRuleService.createRule(USER_ID,
                new BankCategoryRuleEditDTO(" weekly ", "  blue   mart ", 3L, null, true));

        ArgumentCaptor<BankCategoryRule> captor = ArgumentCaptor.forClass(BankCategoryRule.class);
        verify(this.bankCategoryRuleRepository).saveAndFlush(captor.capture());
        assertEquals("blue   mart", captor.getValue().getPattern());
        assertEquals("BLUE MART", captor.getValue().getNormalizedPattern());
        assertEquals("weekly", captor.getValue().getName());
        assertEquals("Groceries", created.getCategoryName());
    }

    @Test
    void createRule_rejectsAPatternThatAlreadyExists() {
        when(this.bankCategoryRuleRepository.findByUserIdAndNormalizedPattern(USER_ID, "BLUEMART"))
                .thenReturn(Optional.of(BankCategoryRule.builder().id(9L).build()));

        assertThrows(ValidationException.class, () -> this.bankCategoryRuleService.createRule(USER_ID,
                new BankCategoryRuleEditDTO(null, "BlueMart", 3L, null, true)));
        verify(this.bankCategoryRuleRepository, never()).saveAndFlush(any());
    }

    @Test
    void createRule_rejectsAPatternWithNoUsableCharacters() {
        assertThrows(ValidationException.class, () -> this.bankCategoryRuleService.createRule(USER_ID,
                new BankCategoryRuleEditDTO(null, "   ", 3L, null, true)));
    }

    @Test
    void createRule_failsWhenTheCategoryBelongsToSomebodyElse() {
        when(this.spendingCategoryRepository.findByIdAndUserId(8L, USER_ID)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> this.bankCategoryRuleService.createRule(USER_ID,
                new BankCategoryRuleEditDTO(null, "BlueMart", 8L, null, true)));
    }

    @Test
    void createRule_translatesTheDatabaseConstraintIntoAValidationError() {
        when(this.bankCategoryRuleRepository.saveAndFlush(any(BankCategoryRule.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThrows(ValidationException.class, () -> this.bankCategoryRuleService.createRule(USER_ID,
                new BankCategoryRuleEditDTO(null, "BlueMart", 3L, null, true)));
    }

    @Test
    void updateRule_movesTheRuleToAnotherCategory() {
        BankCategoryRule existing = this.rule("BlueMart", 0, this.groceries);
        when(this.bankCategoryRuleRepository.findByIdAndUserId(0L, USER_ID)).thenReturn(Optional.of(existing));

        BankCategoryRuleDTO updated = this.bankCategoryRuleService.updateRule(USER_ID, 0L,
                new BankCategoryRuleEditDTO(null, "BlueMart", 4L, 7, false));

        assertEquals("Travel", updated.getCategoryName());
        assertEquals(7, updated.getPriority());
        assertEquals(false, updated.isActive());
    }

    @Test
    void matcherFor_onlyLoadsTheActiveRules() {
        when(this.bankCategoryRuleRepository.findByUserIdAndActiveTrueOrderByPriorityAscPatternAsc(USER_ID))
                .thenReturn(List.of(this.rule("BlueMart", 0, this.groceries)));

        CategoryRuleMatcher matcher = this.bankCategoryRuleService.matcherFor(USER_ID);

        assertSame(this.groceries, matcher.match("BLUEMART 118 Riverton"));
    }

    @Test
    void applyRules_categorizesTheTransactionsThatHaveNoCategoryYet() {
        BankTransaction unmatched = this.transaction(1L, "BLUEMART 118 Riverton   HU", null, false);
        when(this.bankCategoryRuleRepository.findByUserIdAndActiveTrueOrderByPriorityAscPatternAsc(USER_ID))
                .thenReturn(List.of(this.rule("BlueMart", 0, this.groceries)));
        when(this.bankTransactionRepository.findByUserIdAndCategoryLockedFalse(USER_ID))
                .thenReturn(List.of(unmatched));

        CategorizeResultDTO result = this.bankCategoryRuleService.applyRules(USER_ID);

        assertEquals(1, result.getCategorized());
        assertEquals(0, result.getCleared());
        assertSame(this.groceries, unmatched.getCategory());
    }

    @Test
    void applyRules_leavesATransactionWhoseCategoryWasSetByHand() {
        BankTransaction locked = this.transaction(1L, "BLUEMART 118 Riverton   HU", this.travel, true);
        when(this.bankCategoryRuleRepository.findByUserIdAndActiveTrueOrderByPriorityAscPatternAsc(USER_ID))
                .thenReturn(List.of(this.rule("BlueMart", 0, this.groceries)));
        when(this.bankTransactionRepository.findByUserIdAndCategoryLockedFalse(USER_ID))
                .thenReturn(List.of());

        CategorizeResultDTO result = this.bankCategoryRuleService.applyRules(USER_ID);

        assertEquals(0, result.getCategorized());
        assertSame(this.travel, locked.getCategory());
    }

    @Test
    void applyRules_clearsACategoryThatNoRuleMatchesAnyMore() {
        BankTransaction stale = this.transaction(1L, "CORNER BAKERY   RIVERTON  HU", this.groceries, false);
        when(this.bankCategoryRuleRepository.findByUserIdAndActiveTrueOrderByPriorityAscPatternAsc(USER_ID))
                .thenReturn(List.of(this.rule("BlueMart", 0, this.groceries)));
        when(this.bankTransactionRepository.findByUserIdAndCategoryLockedFalse(USER_ID))
                .thenReturn(List.of(stale));

        CategorizeResultDTO result = this.bankCategoryRuleService.applyRules(USER_ID);

        assertEquals(0, result.getCategorized());
        assertEquals(1, result.getCleared());
        assertNull(stale.getCategory());
    }

    @Test
    void applyRules_countsNothingWhenTheCategoryIsAlreadyRight() {
        BankTransaction settled = this.transaction(1L, "BLUEMART 118 Riverton   HU", this.groceries, false);
        when(this.bankCategoryRuleRepository.findByUserIdAndActiveTrueOrderByPriorityAscPatternAsc(USER_ID))
                .thenReturn(List.of(this.rule("BlueMart", 0, this.groceries)));
        when(this.bankTransactionRepository.findByUserIdAndCategoryLockedFalse(USER_ID))
                .thenReturn(List.of(settled));

        CategorizeResultDTO result = this.bankCategoryRuleService.applyRules(USER_ID);

        assertEquals(0, result.getCategorized());
        assertEquals(0, result.getCleared());
        assertSame(this.groceries, settled.getCategory());
    }
}
