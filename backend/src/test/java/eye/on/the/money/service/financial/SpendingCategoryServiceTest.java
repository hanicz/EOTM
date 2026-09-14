package eye.on.the.money.service.financial;

import eye.on.the.money.dto.in.SpendingCategoryEditDTO;
import eye.on.the.money.dto.out.SpendingCategoryDTO;
import eye.on.the.money.exception.ValidationException;
import eye.on.the.money.model.User;
import eye.on.the.money.model.financial.BankCategoryRule;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SpendingCategoryServiceTest {

    private static final Long USER_ID = 1L;

    @Mock
    private SpendingCategoryRepository spendingCategoryRepository;
    @Mock
    private BankCategoryRuleRepository bankCategoryRuleRepository;
    @Mock
    private BankTransactionRepository bankTransactionRepository;
    @Mock
    private UserService userService;

    @InjectMocks
    private SpendingCategoryService spendingCategoryService;

    private final User user = User.builder().id(USER_ID).email("test@email.com").build();

    @BeforeEach
    void setUp() {
        when(this.userService.getReference(USER_ID)).thenReturn(this.user);
        when(this.spendingCategoryRepository.saveAndFlush(any(SpendingCategory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(this.spendingCategoryRepository.save(any(SpendingCategory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(this.spendingCategoryRepository.findByUserIdAndNormalizedName(anyLong(), anyString()))
                .thenReturn(Optional.empty());
        when(this.spendingCategoryRepository.findByUserIdOrderByPositionAscNameAsc(USER_ID))
                .thenReturn(List.of());
    }

    @Test
    void createCategory_trimsTheNameAndStoresItsNormalisedForm() {
        SpendingCategoryDTO created = this.spendingCategoryService.createCategory(USER_ID,
                new SpendingCategoryEditDTO("  Weekly  shop ", CategoryColor.GREEN, null));

        ArgumentCaptor<SpendingCategory> captor = ArgumentCaptor.forClass(SpendingCategory.class);
        verify(this.spendingCategoryRepository).saveAndFlush(captor.capture());
        assertEquals("Weekly  shop", captor.getValue().getName());
        assertEquals("WEEKLY SHOP", captor.getValue().getNormalizedName());
        assertEquals(CategoryColor.GREEN, created.getColor());
    }

    @Test
    void createCategory_rejectsANameThatAlreadyExists() {
        when(this.spendingCategoryRepository.findByUserIdAndNormalizedName(USER_ID, "GROCERIES"))
                .thenReturn(Optional.of(SpendingCategory.builder().id(7L).build()));

        assertThrows(ValidationException.class, () -> this.spendingCategoryService.createCategory(USER_ID,
                new SpendingCategoryEditDTO("Groceries", CategoryColor.GREEN, null)));
        verify(this.spendingCategoryRepository, never()).saveAndFlush(any());
    }

    @Test
    void createCategory_rejectsANameWithNoUsableCharacters() {
        assertThrows(ValidationException.class, () -> this.spendingCategoryService.createCategory(USER_ID,
                new SpendingCategoryEditDTO("   ", CategoryColor.GREEN, null)));
    }

    @Test
    void createCategory_translatesTheDatabaseConstraintIntoAValidationError() {
        when(this.spendingCategoryRepository.saveAndFlush(any(SpendingCategory.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThrows(ValidationException.class, () -> this.spendingCategoryService.createCategory(USER_ID,
                new SpendingCategoryEditDTO("Groceries", CategoryColor.GREEN, null)));
    }

    @Test
    void updateCategory_failsWhenTheCategoryBelongsToSomebodyElse() {
        when(this.spendingCategoryRepository.findByIdAndUserId(9L, USER_ID)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> this.spendingCategoryService.updateCategory(USER_ID, 9L,
                new SpendingCategoryEditDTO("Groceries", CategoryColor.GREEN, null)));
    }

    @Test
    void updateCategory_allowsACategoryToKeepItsOwnName() {
        SpendingCategory existing = SpendingCategory.builder().id(3L).name("Groceries")
                .normalizedName("GROCERIES").color(CategoryColor.GREEN).position(0).build();
        when(this.spendingCategoryRepository.findByIdAndUserId(3L, USER_ID)).thenReturn(Optional.of(existing));
        when(this.spendingCategoryRepository.findByUserIdAndNormalizedName(USER_ID, "GROCERIES"))
                .thenReturn(Optional.of(existing));

        SpendingCategoryDTO updated = this.spendingCategoryService.updateCategory(USER_ID, 3L,
                new SpendingCategoryEditDTO("Groceries", CategoryColor.ORANGE, null));

        assertEquals(CategoryColor.ORANGE, updated.getColor());
    }

    @Test
    void deleteCategoriesByIds_unlinksTheTransactionsAndDropsTheRulesFirst() {
        List<Long> ids = List.of(3L, 4L);

        this.spendingCategoryService.deleteCategoriesByIds(USER_ID, ids);

        verify(this.bankTransactionRepository).clearCategoryByUserIdAndCategoryIdIn(USER_ID, ids);
        verify(this.bankCategoryRuleRepository).deleteByUserIdAndCategoryIdIn(USER_ID, ids);
        verify(this.spendingCategoryRepository).deleteByUserIdAndIdIn(USER_ID, ids);
    }

    @Test
    void createStarterSet_createsEveryCategoryAndRule() {
        when(this.spendingCategoryRepository.countByUserId(USER_ID)).thenReturn(0L);

        this.spendingCategoryService.createStarterSet(USER_ID);

        verify(this.spendingCategoryRepository, times(StarterCategories.DEFINITIONS.size()))
                .save(any(SpendingCategory.class));

        ArgumentCaptor<List<BankCategoryRule>> captor = ArgumentCaptor.forClass(List.class);
        verify(this.bankCategoryRuleRepository).saveAll(captor.capture());
        int expected = StarterCategories.DEFINITIONS.stream().mapToInt(definition -> definition.patterns().size()).sum();
        assertEquals(expected, captor.getValue().size());
    }

    @Test
    void createStarterSet_ordersTheLongestPatternFirstSoASpecificRuleWins() {
        when(this.spendingCategoryRepository.countByUserId(USER_ID)).thenReturn(0L);

        this.spendingCategoryService.createStarterSet(USER_ID);

        ArgumentCaptor<List<BankCategoryRule>> captor = ArgumentCaptor.forClass(List.class);
        verify(this.bankCategoryRuleRepository).saveAll(captor.capture());
        List<BankCategoryRule> rules = captor.getValue();
        for (int index = 1; index < rules.size(); index++) {
            assertEquals(index, rules.get(index).getPriority());
            assertTrue(rules.get(index - 1).getNormalizedPattern().length()
                    >= rules.get(index).getNormalizedPattern().length());
        }
    }

    @Test
    void createStarterSet_isRefusedOnceThereAreCategories() {
        when(this.spendingCategoryRepository.countByUserId(USER_ID)).thenReturn(3L);

        assertThrows(ValidationException.class, () -> this.spendingCategoryService.createStarterSet(USER_ID));
        verify(this.bankCategoryRuleRepository, never()).saveAll(any());
    }
}
