package eye.on.the.money.service.financial;

import eye.on.the.money.dto.in.SpendingCategoryEditDTO;
import eye.on.the.money.model.financial.CategoryColor;
import eye.on.the.money.model.financial.SpendingCategory;
import eye.on.the.money.repository.financial.BankCategoryRuleRepository;
import eye.on.the.money.repository.financial.BankTransactionRepository;
import eye.on.the.money.repository.financial.SpendingCategoryRepository;
import eye.on.the.money.repository.forex.CurrencyRepository;
import eye.on.the.money.service.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringJUnitConfig(BankReportCacheTest.CacheTestConfig.class)
class BankReportCacheTest {

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;

    @Configuration
    @EnableCaching(proxyTargetClass = true)
    @Import({BankTransactionService.class, BankCategoryRuleService.class, SpendingCategoryService.class})
    static class CacheTestConfig {

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager();
        }
    }

    @MockitoBean
    private BankTransactionRepository bankTransactionRepository;
    @MockitoBean
    private SpendingCategoryRepository spendingCategoryRepository;
    @MockitoBean
    private BankCategoryRuleRepository bankCategoryRuleRepository;
    @MockitoBean
    private CurrencyRepository currencyRepository;
    @MockitoBean
    private BankExclusionRuleService bankExclusionRuleService;
    @MockitoBean
    private UserService userService;
    @MockitoBean
    private ModelMapper modelMapper;

    @Autowired
    private BankTransactionService bankTransactionService;
    @Autowired
    private BankCategoryRuleService bankCategoryRuleService;
    @Autowired
    private SpendingCategoryService spendingCategoryService;
    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        this.cacheManager.getCacheNames().forEach(name -> this.cacheManager.getCache(name).clear());
        when(this.bankTransactionRepository.findMonthlyCashFlow(anyLong())).thenReturn(List.of());
        when(this.bankTransactionRepository.findMonthlyIncome(anyLong())).thenReturn(List.of());
        when(this.bankTransactionRepository.findMonthlyCategorySpending(anyLong())).thenReturn(List.of());
    }

    private void readAllReports(Long userId) {
        this.bankTransactionService.getMonthlyCashFlow(userId);
        this.bankTransactionService.getMonthlyIncome(userId);
        this.bankTransactionService.getMonthlyCategorySpending(userId);
        this.bankTransactionService.getYearlyCashFlow(userId);
    }

    private void verifyReportQueries(Long userId, int monthly, int income, int category) {
        verify(this.bankTransactionRepository, times(monthly)).findMonthlyCashFlow(userId);
        verify(this.bankTransactionRepository, times(income)).findMonthlyIncome(userId);
        verify(this.bankTransactionRepository, times(category)).findMonthlyCategorySpending(userId);
    }

    private void assertReportsReloadAfter(Runnable change) {
        this.readAllReports(USER_ID);
        clearInvocations(this.bankTransactionRepository);

        change.run();
        this.readAllReports(USER_ID);

        this.verifyReportQueries(USER_ID, 2, 1, 1);
    }

    @Test
    void readsEachReportFromTheDatabaseOnlyOnce() {
        this.readAllReports(USER_ID);
        this.readAllReports(USER_ID);

        this.verifyReportQueries(USER_ID, 2, 1, 1);
    }

    @Test
    void keepsEachUsersReportsApart() {
        this.bankTransactionService.getMonthlyCashFlow(USER_ID);
        this.bankTransactionService.getMonthlyCashFlow(OTHER_USER_ID);

        verify(this.bankTransactionRepository, times(1)).findMonthlyCashFlow(USER_ID);
        verify(this.bankTransactionRepository, times(1)).findMonthlyCashFlow(OTHER_USER_ID);
    }

    @Test
    void excludingATransactionClearsTheReports() {
        this.assertReportsReloadAfter(() -> this.bankTransactionService.setExcluded(USER_ID, List.of(5L), true));
    }

    @Test
    void settingACategoryClearsTheReports() {
        this.assertReportsReloadAfter(() -> this.bankTransactionService.setCategory(USER_ID, List.of(5L), null));
    }

    @Test
    void deletingTransactionsClearsTheReports() {
        this.assertReportsReloadAfter(() -> this.bankTransactionService.deleteTransactionById(USER_ID, List.of(5L)));
    }

    @Test
    void reapplyingCategoryRulesClearsTheReports() {
        this.assertReportsReloadAfter(() -> this.bankCategoryRuleService.applyRules(USER_ID));
    }

    @Test
    void renamingACategoryClearsTheReports() {
        SpendingCategory category = SpendingCategory.builder().id(3L).name("Groceries")
                .normalizedName("GROCERIES").color(CategoryColor.GREEN).build();
        when(this.spendingCategoryRepository.findByIdAndUserId(3L, USER_ID)).thenReturn(Optional.of(category));
        when(this.spendingCategoryRepository.findByUserIdAndNormalizedName(anyLong(), anyString()))
                .thenReturn(Optional.empty());
        when(this.spendingCategoryRepository.saveAndFlush(any(SpendingCategory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        this.assertReportsReloadAfter(() -> this.spendingCategoryService.updateCategory(USER_ID, 3L,
                new SpendingCategoryEditDTO("Food", CategoryColor.ORANGE, null)));
    }

    @Test
    void deletingACategoryClearsTheReports() {
        this.assertReportsReloadAfter(() -> this.spendingCategoryService.deleteCategoriesByIds(USER_ID, List.of(3L)));
    }

    @Test
    void changingOneUserLeavesTheOtherUsersReportsCached() {
        this.bankTransactionService.getMonthlyCashFlow(OTHER_USER_ID);

        this.bankTransactionService.setExcluded(USER_ID, List.of(5L), true);
        this.bankTransactionService.getMonthlyCashFlow(OTHER_USER_ID);

        verify(this.bankTransactionRepository, times(1)).findMonthlyCashFlow(OTHER_USER_ID);
    }
}
