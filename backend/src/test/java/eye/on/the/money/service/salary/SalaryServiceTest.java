package eye.on.the.money.service.salary;

import eye.on.the.money.dto.in.SalaryEditDTO;
import eye.on.the.money.dto.out.SalaryDTO;
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
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    void deleteSalariesByIds_scopesTheDeleteToTheUser() {
        this.salaryService.deleteSalariesByIds(USER_ID, List.of(1L, 2L));

        verify(this.salaryRepository).deleteByUserIdAndIdIn(USER_ID, List.of(1L, 2L));
    }
}
