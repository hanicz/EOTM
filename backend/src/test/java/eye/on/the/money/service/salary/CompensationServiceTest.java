package eye.on.the.money.service.salary;

import eye.on.the.money.dto.in.CompensationCompareDTO;
import eye.on.the.money.dto.in.CompensationEditDTO;
import eye.on.the.money.dto.in.CompensationItemInputDTO;
import eye.on.the.money.dto.out.CompensationItemDTO;
import eye.on.the.money.dto.out.CompensationPackageDTO;
import eye.on.the.money.dto.out.SalaryDTO;
import eye.on.the.money.exception.ValidationException;
import eye.on.the.money.model.User;
import eye.on.the.money.model.salary.*;
import eye.on.the.money.repository.salary.CompensationRepository;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CompensationServiceTest {

    private static final Long USER_ID = 1L;
    private static final LocalDate FROM = LocalDate.of(2024, 6, 1);

    @Mock
    private CompensationRepository compensationRepository;
    @Mock
    private SalaryService salaryService;
    @Mock
    private UserService userService;

    @Spy
    private SalaryTaxCalculator salaryTaxCalculator = new SalaryTaxCalculator();

    @InjectMocks
    private CompensationService compensationService;

    private final User user = User.builder().id(USER_ID).email("test@email.com").build();

    @BeforeEach
    void setUp() {
        when(this.userService.getReference(USER_ID)).thenReturn(this.user);
        when(this.compensationRepository.saveAndFlush(any(CompensationItem.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(this.salaryService.getSalaries(USER_ID)).thenReturn(List.of(this.currentSalary("600000")));
    }

    private SalaryDTO currentSalary(String grossMonthly) {
        BigDecimal monthly = new BigDecimal(grossMonthly);
        return SalaryDTO.builder()
                .id(7L)
                .basis(SalaryBasis.MONTHLY)
                .currencyId("HUF")
                .validFrom(FROM)
                .dependents(0)
                .note("Sample role")
                .grossMonthly(monthly)
                .grossAnnual(monthly.multiply(BigDecimal.valueOf(12)))
                .netMonthly(new BigDecimal("399000"))
                .netAnnual(new BigDecimal("4788000"))
                .build();
    }

    private CompensationItem stored(String name, String monthlyAmount, CompensationTaxTreatment taxTreatment) {
        return CompensationItem.builder().id(11L).name(name)
                .amountMode(CompensationAmountMode.MONTHLY_AMOUNT).monthlyAmount(new BigDecimal(monthlyAmount))
                .taxTreatment(taxTreatment).user(this.user).build();
    }

    private CompensationItem storedPercent(String percent, CompensationTaxTreatment taxTreatment) {
        return CompensationItem.builder().id(12L).name("Annual bonus")
                .amountMode(CompensationAmountMode.PERCENT_OF_ANNUAL).percent(new BigDecimal(percent))
                .taxTreatment(taxTreatment).user(this.user).build();
    }

    private CompensationEditDTO editDTO(CompensationAmountMode mode, String monthlyAmount, String percent) {
        return new CompensationEditDTO("  Annual bonus  ", mode,
                monthlyAmount == null ? null : new BigDecimal(monthlyAmount),
                percent == null ? null : new BigDecimal(percent),
                CompensationTaxTreatment.TAXED_AS_SALARY, "  Every February  ");
    }

    private CompensationCompareDTO compareDTO(String baseAmount, List<CompensationItemInputDTO> items) {
        return new CompensationCompareDTO("The offer", new BigDecimal(baseAmount), SalaryBasis.ANNUAL,
                "HUF", 0, items);
    }

    @Test
    void getCurrentPackage_returnsNothingWhenNoSalaryIsRecorded() {
        when(this.salaryService.getSalaries(USER_ID)).thenReturn(List.of());

        assertTrue(this.compensationService.getCurrentPackage(USER_ID).isEmpty());
    }

    @Test
    void getCurrentPackage_takesTheBaseFiguresFromTheCurrentSalary() {
        when(this.compensationRepository.findByUserIdOrderByNameAscIdAsc(USER_ID)).thenReturn(List.of());

        CompensationPackageDTO pkg = this.compensationService.getCurrentPackage(USER_ID).orElseThrow();

        assertEquals("HUF", pkg.getCurrencyId());
        assertEquals(0, new BigDecimal("600000").compareTo(pkg.getBaseGrossMonthly()));
        assertEquals(0, new BigDecimal("7200000").compareTo(pkg.getTotalGrossAnnual()));
        assertEquals(0, new BigDecimal("399000").compareTo(pkg.getTotalNetMonthly()));
    }

    @Test
    void getCurrentPackage_taxesAnItemTakenAsSalaryAtTheFlatMarginalRate() {
        when(this.compensationRepository.findByUserIdOrderByNameAscIdAsc(USER_ID))
                .thenReturn(List.of(this.stored("Annual bonus", "100000",
                        CompensationTaxTreatment.TAXED_AS_SALARY)));

        CompensationItemDTO item = this.compensationService.getCurrentPackage(USER_ID).orElseThrow()
                .getItems().get(0);

        assertEquals(0, new BigDecimal("100000").compareTo(item.getGrossMonthly()));
        assertEquals(0, new BigDecimal("66500").compareTo(item.getNetMonthly()));
        assertEquals(0, new BigDecimal("798000").compareTo(item.getNetAnnual()));
    }

    @Test
    void getCurrentPackage_leavesATaxFreeItemWhole() {
        when(this.compensationRepository.findByUserIdOrderByNameAscIdAsc(USER_ID))
                .thenReturn(List.of(this.stored("Phone", "8000", CompensationTaxTreatment.TAX_FREE)));

        CompensationItemDTO item = this.compensationService.getCurrentPackage(USER_ID).orElseThrow()
                .getItems().get(0);

        assertEquals(0, new BigDecimal("8000").compareTo(item.getGrossMonthly()));
        assertEquals(0, new BigDecimal("8000").compareTo(item.getNetMonthly()));
    }

    @Test
    void getCurrentPackage_leavesNoGrossOnAnItemReceivedNet() {
        when(this.compensationRepository.findByUserIdOrderByNameAscIdAsc(USER_ID))
                .thenReturn(List.of(this.stored("SZEP card", "37500",
                        CompensationTaxTreatment.RECEIVED_NET)));

        CompensationPackageDTO pkg = this.compensationService.getCurrentPackage(USER_ID).orElseThrow();
        CompensationItemDTO item = pkg.getItems().get(0);

        assertNull(item.getGrossMonthly());
        assertNull(item.getGrossAnnual());
        assertEquals(0, new BigDecimal("37500").compareTo(item.getNetMonthly()));
        assertEquals(0, new BigDecimal("436500").compareTo(pkg.getTotalNetMonthly()));
    }

    @Test
    void getCurrentPackage_addsTheExtrasOnTopOfTheBase() {
        when(this.compensationRepository.findByUserIdOrderByNameAscIdAsc(USER_ID))
                .thenReturn(List.of(this.stored("Phone", "8000", CompensationTaxTreatment.TAX_FREE),
                        this.stored("Meal", "12000", CompensationTaxTreatment.TAX_FREE)));

        CompensationPackageDTO pkg = this.compensationService.getCurrentPackage(USER_ID).orElseThrow();

        assertEquals(0, new BigDecimal("620000").compareTo(pkg.getTotalGrossMonthly()));
        assertEquals(0, new BigDecimal("7440000").compareTo(pkg.getTotalGrossAnnual()));
        assertEquals(0, new BigDecimal("419000").compareTo(pkg.getTotalNetMonthly()));
    }

    @Test
    void getCurrentPackage_resolvesAPercentageAgainstTheAnnualBase() {
        when(this.compensationRepository.findByUserIdOrderByNameAscIdAsc(USER_ID))
                .thenReturn(List.of(this.storedPercent("15", CompensationTaxTreatment.TAXED_AS_SALARY)));

        CompensationItemDTO item = this.compensationService.getCurrentPackage(USER_ID).orElseThrow()
                .getItems().get(0);

        assertEquals(0, new BigDecimal("90000").compareTo(item.getGrossMonthly()));
        assertEquals(0, new BigDecimal("1080000").compareTo(item.getGrossAnnual()));
        assertEquals(0, new BigDecimal("15").compareTo(item.getPercent()));
        assertNull(item.getMonthlyAmount());
    }

    @Test
    void comparePackage_pricesADraftWithoutTouchingTheDatabase() {
        CompensationPackageDTO pkg = this.compensationService.comparePackage(
                this.compareDTO("9600000", List.of()));

        assertEquals(0, new BigDecimal("800000").compareTo(pkg.getBaseGrossMonthly()));
        assertEquals(0, new BigDecimal("9600000").compareTo(pkg.getTotalGrossAnnual()));
        assertEquals(0, new BigDecimal("532000").compareTo(pkg.getTotalNetMonthly()));
        assertNull(pkg.getValidFrom());
        verify(this.compensationRepository, never()).saveAndFlush(any(CompensationItem.class));
        verify(this.compensationRepository, never()).findByUserIdOrderByNameAscIdAsc(any());
    }

    @Test
    void comparePackage_takesAnEmptyItemListWhenNoneIsSent() {
        CompensationPackageDTO pkg = this.compensationService.comparePackage(
                new CompensationCompareDTO(null, new BigDecimal("9600000"), SalaryBasis.ANNUAL, "HUF", 0, null));

        assertTrue(pkg.getItems().isEmpty());
    }

    @Test
    void comparePackage_growsAPercentageItemWithTheDraftsHigherBase() {
        CompensationItemInputDTO bonus = new CompensationItemInputDTO("Annual bonus",
                CompensationAmountMode.PERCENT_OF_ANNUAL, null, new BigDecimal("15"),
                CompensationTaxTreatment.TAXED_AS_SALARY);
        when(this.compensationRepository.findByUserIdOrderByNameAscIdAsc(USER_ID))
                .thenReturn(List.of(this.storedPercent("15", CompensationTaxTreatment.TAXED_AS_SALARY)));

        BigDecimal mine = this.compensationService.getCurrentPackage(USER_ID).orElseThrow()
                .getItems().get(0).getGrossMonthly();
        BigDecimal theirs = this.compensationService.comparePackage(this.compareDTO("9600000", List.of(bonus)))
                .getItems().get(0).getGrossMonthly();

        assertEquals(0, new BigDecimal("90000").compareTo(mine));
        assertEquals(0, new BigDecimal("120000").compareTo(theirs));
    }

    @Test
    void comparePackage_rejectsAnItemThatCarriesNeitherFigure() {
        CompensationItemInputDTO broken = new CompensationItemInputDTO("Annual bonus",
                CompensationAmountMode.PERCENT_OF_ANNUAL, null, null, CompensationTaxTreatment.TAX_FREE);
        CompensationCompareDTO compareDTO = this.compareDTO("9600000", List.of(broken));

        assertThrows(ValidationException.class, () -> this.compensationService.comparePackage(compareDTO));
    }

    @Test
    void createItem_trimsTheNameAndTheNote() {
        CompensationItemDTO item = this.compensationService.createItem(USER_ID,
                this.editDTO(CompensationAmountMode.MONTHLY_AMOUNT, "100000", null));

        assertEquals("Annual bonus", item.getName());
        assertEquals("Every February", item.getNote());
        assertEquals(0, new BigDecimal("66500").compareTo(item.getNetMonthly()));
    }

    @Test
    void createItem_pricesAPercentageAgainstTheCurrentSalary() {
        CompensationItemDTO item = this.compensationService.createItem(USER_ID,
                this.editDTO(CompensationAmountMode.PERCENT_OF_ANNUAL, null, "15"));

        assertEquals(0, new BigDecimal("90000").compareTo(item.getGrossMonthly()));
    }

    @Test
    void createItem_stillSavesWhenNoSalaryIsRecordedYet() {
        when(this.salaryService.getSalaries(USER_ID)).thenReturn(List.of());

        CompensationItemDTO item = this.compensationService.createItem(USER_ID,
                this.editDTO(CompensationAmountMode.MONTHLY_AMOUNT, "100000", null));

        assertEquals(0, new BigDecimal("100000").compareTo(item.getGrossMonthly()));
        assertNull(item.getCurrencyId());
    }

    @Test
    void createItem_rejectsAMonthlyItemWithoutAnAmount() {
        CompensationEditDTO editDTO = this.editDTO(CompensationAmountMode.MONTHLY_AMOUNT, null, null);

        assertThrows(ValidationException.class, () -> this.compensationService.createItem(USER_ID, editDTO));
        verify(this.compensationRepository, never()).saveAndFlush(any(CompensationItem.class));
    }

    @Test
    void createItem_rejectsAMonthlyItemThatAlsoCarriesAPercentage() {
        CompensationEditDTO editDTO = this.editDTO(CompensationAmountMode.MONTHLY_AMOUNT, "100000", "15");

        assertThrows(ValidationException.class, () -> this.compensationService.createItem(USER_ID, editDTO));
    }

    @Test
    void createItem_rejectsAPercentageItemWithoutAPercentage() {
        CompensationEditDTO editDTO = this.editDTO(CompensationAmountMode.PERCENT_OF_ANNUAL, null, null);

        assertThrows(ValidationException.class, () -> this.compensationService.createItem(USER_ID, editDTO));
    }

    @Test
    void createItem_rejectsAPercentageItemThatAlsoCarriesAnAmount() {
        CompensationEditDTO editDTO = this.editDTO(CompensationAmountMode.PERCENT_OF_ANNUAL, "100000", "15");

        assertThrows(ValidationException.class, () -> this.compensationService.createItem(USER_ID, editDTO));
    }

    @Test
    void updateItem_doesNotTouchAnotherUsersItem() {
        when(this.compensationRepository.findByIdAndUserId(11L, USER_ID)).thenReturn(Optional.empty());
        CompensationEditDTO editDTO = this.editDTO(CompensationAmountMode.MONTHLY_AMOUNT, "100000", null);

        assertThrows(NoSuchElementException.class, () -> this.compensationService.updateItem(USER_ID, 11L, editDTO));
    }

    @Test
    void updateItem_swapsAMonthlyAmountForAPercentage() {
        when(this.compensationRepository.findByIdAndUserId(11L, USER_ID))
                .thenReturn(Optional.of(this.stored("Annual bonus", "100000",
                        CompensationTaxTreatment.TAXED_AS_SALARY)));

        CompensationItemDTO item = this.compensationService.updateItem(USER_ID, 11L,
                this.editDTO(CompensationAmountMode.PERCENT_OF_ANNUAL, null, "15"));

        assertEquals(CompensationAmountMode.PERCENT_OF_ANNUAL, item.getAmountMode());
        assertNull(item.getMonthlyAmount());
        assertEquals(0, new BigDecimal("90000").compareTo(item.getGrossMonthly()));
    }

    @Test
    void deleteItemsByIds_scopesTheDeleteToTheUser() {
        this.compensationService.deleteItemsByIds(USER_ID, List.of(11L, 12L));

        verify(this.compensationRepository).deleteByUserIdAndIdIn(USER_ID, List.of(11L, 12L));
    }
}
