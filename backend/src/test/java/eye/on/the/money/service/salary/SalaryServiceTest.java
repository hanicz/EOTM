package eye.on.the.money.service.salary;

import eye.on.the.money.dto.in.SalaryEditDTO;
import eye.on.the.money.dto.out.SalaryDTO;
import eye.on.the.money.dto.out.SalaryRaiseDTO;
import eye.on.the.money.dto.out.SalaryRaiseScenarioDTO;
import eye.on.the.money.exception.ValidationException;
import eye.on.the.money.model.Currency;
import eye.on.the.money.model.User;
import eye.on.the.money.model.salary.Salary;
import eye.on.the.money.model.salary.SalaryBasis;
import eye.on.the.money.repository.forex.CurrencyRepository;
import eye.on.the.money.repository.salary.SalaryRepository;
import eye.on.the.money.service.shared.SalaryTaxCalculator;
import eye.on.the.money.service.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SalaryServiceTest {

    private static final Long USER_ID = 1L;
    private static final LocalDate FROM = LocalDate.of(2024, 6, 1);

    @Mock
    private SalaryRepository salaryRepository;
    @Mock
    private CurrencyRepository currencyRepository;
    @Mock
    private UserService userService;

    @Spy
    private SalaryTaxCalculator salaryTaxCalculator = new SalaryTaxCalculator();

    @InjectMocks
    private SalaryService salaryService;

    private final User user = User.builder().id(USER_ID).email("test@email.com").build();
    private final Currency huf = new Currency("HUF", "forint");

    @BeforeEach
    void setUp() {
        when(this.userService.getReference(USER_ID)).thenReturn(this.user);
        when(this.currencyRepository.findById("HUF")).thenReturn(Optional.of(this.huf));
        when(this.currencyRepository.findById("XXX")).thenReturn(Optional.empty());
        when(this.salaryRepository.saveAndFlush(any(Salary.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private SalaryEditDTO editDTO(String amount, SalaryBasis basis, int dependents, LocalDate validTo) {
        return new SalaryEditDTO(new BigDecimal(amount), basis, "HUF", FROM, validTo, dependents, "  Sample role  ");
    }

    private Salary stored(String amount, SalaryBasis basis, int dependents) {
        return Salary.builder().id(7L).amount(new BigDecimal(amount)).basis(basis).validFrom(FROM)
                .dependents(dependents).currency(this.huf).user(this.user).build();
    }

    @Test
    void createSalary_derivesBothGrossesFromAMonthlyAmount() {
        SalaryDTO dto = this.salaryService.createSalary(USER_ID, this.editDTO("600000", SalaryBasis.MONTHLY, 0, null));

        assertEquals(0, new BigDecimal("600000").compareTo(dto.getGrossMonthly()));
        assertEquals(0, new BigDecimal("7200000").compareTo(dto.getGrossAnnual()));
        assertEquals(0, new BigDecimal("399000").compareTo(dto.getNetMonthly()));
        assertEquals(0, new BigDecimal("4788000").compareTo(dto.getNetAnnual()));
    }

    @Test
    void createSalary_derivesTheMonthlyGrossFromAnAnnualAmount() {
        SalaryDTO dto = this.salaryService.createSalary(USER_ID, this.editDTO("7200000", SalaryBasis.ANNUAL, 0, null));

        assertEquals(0, new BigDecimal("600000").compareTo(dto.getGrossMonthly()));
        assertEquals(0, new BigDecimal("7200000").compareTo(dto.getGrossAnnual()));
        assertEquals(0, new BigDecimal("399000").compareTo(dto.getNetMonthly()));
    }

    @Test
    void createSalary_trimsTheNoteAndKeepsTheDependents() {
        SalaryDTO dto = this.salaryService.createSalary(USER_ID, this.editDTO("600000", SalaryBasis.MONTHLY, 2, null));

        assertEquals("Sample role", dto.getNote());
        assertEquals(2, dto.getDependents());
        assertEquals(0, new BigDecimal("478998").compareTo(dto.getNetMonthly()));
    }

    @Test
    void createSalary_turnsAnEmptyNoteIntoNothing() {
        SalaryEditDTO editDTO = new SalaryEditDTO(new BigDecimal("500000"), SalaryBasis.MONTHLY, "HUF",
                FROM, null, 0, "   ");

        assertNull(this.salaryService.createSalary(USER_ID, editDTO).getNote());
    }

    @Test
    void createSalary_rejectsAPeriodThatEndsBeforeItStarts() {
        SalaryEditDTO editDTO = this.editDTO("600000", SalaryBasis.MONTHLY, 0, FROM.minusDays(1));

        assertThrows(ValidationException.class, () -> this.salaryService.createSalary(USER_ID, editDTO));
        verify(this.salaryRepository, never()).saveAndFlush(any(Salary.class));
    }

    @Test
    void createSalary_rejectsAnUnknownCurrency() {
        SalaryEditDTO editDTO = new SalaryEditDTO(new BigDecimal("600000"), SalaryBasis.MONTHLY, "XXX",
                FROM, null, 0, null);

        assertThrows(NoSuchElementException.class, () -> this.salaryService.createSalary(USER_ID, editDTO));
    }

    @Test
    void updateSalary_doesNotTouchAnotherUsersSalary() {
        when(this.salaryRepository.findByIdAndUserId(7L, USER_ID)).thenReturn(Optional.empty());
        SalaryEditDTO editDTO = this.editDTO("600000", SalaryBasis.MONTHLY, 0, null);

        assertThrows(NoSuchElementException.class, () -> this.salaryService.updateSalary(USER_ID, 7L, editDTO));
    }

    @Test
    void updateSalary_writesTheNewValuesOntoTheStoredRecord() {
        when(this.salaryRepository.findByIdAndUserId(7L, USER_ID))
                .thenReturn(Optional.of(this.stored("600000", SalaryBasis.MONTHLY, 0)));

        SalaryDTO dto = this.salaryService.updateSalary(USER_ID, 7L,
                this.editDTO("900000", SalaryBasis.MONTHLY, 1, LocalDate.of(2026, 1, 31)));

        assertEquals(0, new BigDecimal("900000").compareTo(dto.getGrossMonthly()));
        assertEquals(1, dto.getDependents());
        assertEquals(LocalDate.of(2026, 1, 31), dto.getValidTo());
    }

    @Test
    void getSalaries_convertsEveryRecordOfTheUser() {
        when(this.salaryRepository.findByUserIdOrderByValidFromDesc(USER_ID))
                .thenReturn(List.of(this.stored("600000", SalaryBasis.MONTHLY, 3)));

        List<SalaryDTO> salaries = this.salaryService.getSalaries(USER_ID);

        assertEquals(1, salaries.size());
        assertEquals(0, new BigDecimal("597000").compareTo(salaries.get(0).getNetMonthly()));
    }

    @Test
    void getRaiseScenarios_returnsNothingWhenTheUserHasNoSalary() {
        when(this.salaryRepository.findByUserIdOrderByValidFromDesc(USER_ID)).thenReturn(List.of());

        assertTrue(this.salaryService.getRaiseScenarios(USER_ID).isEmpty());
    }

    @Test
    void getRaiseScenarios_picksTheSalaryThatIsStillRunning() {
        Salary past = this.stored("500000", SalaryBasis.MONTHLY, 0);
        past.setValidTo(FROM.plusMonths(6));
        Salary current = this.stored("600000", SalaryBasis.MONTHLY, 0);
        when(this.salaryRepository.findByUserIdOrderByValidFromDesc(USER_ID)).thenReturn(List.of(past, current));

        SalaryRaiseDTO raise = this.salaryService.getRaiseScenarios(USER_ID).orElseThrow();

        assertEquals(0, new BigDecimal("600000").compareTo(raise.getCurrent().getGrossMonthly()));
        assertNull(raise.getCurrent().getValidTo());
    }

    @Test
    void getRaiseScenarios_fallsBackToTheMostRecentClosedSalary() {
        Salary newest = this.stored("700000", SalaryBasis.MONTHLY, 0);
        newest.setValidTo(FROM.plusYears(1));
        Salary older = this.stored("500000", SalaryBasis.MONTHLY, 0);
        older.setValidTo(FROM.plusMonths(6));
        when(this.salaryRepository.findByUserIdOrderByValidFromDesc(USER_ID)).thenReturn(List.of(newest, older));

        SalaryRaiseDTO raise = this.salaryService.getRaiseScenarios(USER_ID).orElseThrow();

        assertEquals(0, new BigDecimal("700000").compareTo(raise.getCurrent().getGrossMonthly()));
        assertEquals(FROM.plusYears(1), raise.getCurrent().getValidTo());
    }

    @Test
    void getRaiseScenarios_scalesTheGrossByEveryPercentage() {
        when(this.salaryRepository.findByUserIdOrderByValidFromDesc(USER_ID))
                .thenReturn(List.of(this.stored("600000", SalaryBasis.MONTHLY, 0)));

        List<SalaryRaiseScenarioDTO> scenarios = this.salaryService.getRaiseScenarios(USER_ID)
                .orElseThrow().getScenarios();

        assertEquals(List.of(new BigDecimal("2"), new BigDecimal("5"), new BigDecimal("10"),
                new BigDecimal("20"), new BigDecimal("25")), scenarios.stream().map(SalaryRaiseScenarioDTO::getPercent).toList());
        assertEquals(0, new BigDecimal("612000").compareTo(scenarios.get(0).getGrossMonthly()));
        assertEquals(0, new BigDecimal("7344000").compareTo(scenarios.get(0).getGrossAnnual()));
        assertEquals(0, new BigDecimal("750000").compareTo(scenarios.get(4).getGrossMonthly()));
        assertEquals(0, new BigDecimal("9000000").compareTo(scenarios.get(4).getGrossAnnual()));
    }

    @Test
    void getRaiseScenarios_recalculatesTheNetInsteadOfScalingIt() {
        when(this.salaryRepository.findByUserIdOrderByValidFromDesc(USER_ID))
                .thenReturn(List.of(this.stored("600000", SalaryBasis.MONTHLY, 0)));

        SalaryRaiseScenarioDTO tenPercent = this.salaryService.getRaiseScenarios(USER_ID)
                .orElseThrow().getScenarios().get(2);

        assertEquals(0, new BigDecimal("660000").compareTo(tenPercent.getGrossMonthly()));
        assertEquals(0, new BigDecimal("438900").compareTo(tenPercent.getNetMonthly()));
        assertEquals(0, new BigDecimal("5266800").compareTo(tenPercent.getNetAnnual()));
    }

    @Test
    void getRaiseScenarios_liftsTheNetBySmallerStepsWhenAFamilyAllowanceApplies() {
        when(this.salaryRepository.findByUserIdOrderByValidFromDesc(USER_ID))
                .thenReturn(List.of(this.stored("600000", SalaryBasis.MONTHLY, 2)));

        SalaryRaiseDTO raise = this.salaryService.getRaiseScenarios(USER_ID).orElseThrow();
        SalaryRaiseScenarioDTO tenPercent = raise.getScenarios().get(2);

        BigDecimal netGrowth = tenPercent.getNetMonthly()
                .divide(raise.getCurrent().getNetMonthly(), 6, RoundingMode.HALF_UP);

        assertTrue(netGrowth.compareTo(BigDecimal.ONE) > 0);
        assertTrue(netGrowth.compareTo(new BigDecimal("1.1")) < 0);
    }

    @Test
    void getRaiseScenarios_keepsTheFractionsOfANonForintSalary() {
        Salary salary = Salary.builder().id(9L).amount(new BigDecimal("4200.55")).basis(SalaryBasis.MONTHLY)
                .validFrom(FROM).dependents(0).currency(new Currency("EUR", "euro")).user(this.user).build();
        when(this.salaryRepository.findByUserIdOrderByValidFromDesc(USER_ID)).thenReturn(List.of(salary));

        SalaryRaiseScenarioDTO fivePercent = this.salaryService.getRaiseScenarios(USER_ID)
                .orElseThrow().getScenarios().get(1);

        assertEquals(new BigDecimal("4410.58"), fivePercent.getGrossMonthly());
    }

    @Test
    void deleteSalariesByIds_scopesTheDeleteToTheUser() {
        this.salaryService.deleteSalariesByIds(USER_ID, List.of(1L, 2L));

        verify(this.salaryRepository).deleteByUserIdAndIdIn(USER_ID, List.of(1L, 2L));
    }
}
