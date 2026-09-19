package eye.on.the.money.service.salary;

import eye.on.the.money.dto.in.PensionProjectionDTO;
import eye.on.the.money.dto.in.PensionScenarioInputDTO;
import eye.on.the.money.dto.in.PensionStepDTO;
import eye.on.the.money.dto.out.PensionProjectionResultDTO;
import eye.on.the.money.dto.out.PensionScenarioResultDTO;
import eye.on.the.money.dto.out.PensionSensitivityPointDTO;
import eye.on.the.money.dto.out.PensionStopAgePointDTO;
import eye.on.the.money.dto.out.PensionYearDTO;
import eye.on.the.money.exception.ValidationException;
import eye.on.the.money.model.Currency;
import eye.on.the.money.model.salary.DegressioMode;
import eye.on.the.money.model.salary.PensionScenarioType;
import eye.on.the.money.model.salary.Salary;
import eye.on.the.money.model.salary.SalaryBasis;
import eye.on.the.money.repository.salary.SalaryRepository;
import eye.on.the.money.service.shared.SalaryTaxCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.StringWriter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PensionProjectionServiceTest {

    private static final Long USER = 1L;
    private static final double TOLERANCE = 0.01;
    private static final double PENSION_NET = 0.69275;
    private static final BigDecimal GROSS = new BigDecimal("600000");
    private static final double AVERAGE_GROSS = 754_700;

    private static final int THIS_YEAR = LocalDate.now().getYear();

    @Mock
    private SalaryRepository salaryRepository;

    private PensionProjectionService service;

    @BeforeEach
    void setUp() {
        this.service = new PensionProjectionService(this.salaryRepository, new SalaryTaxCalculator());
        this.stubSalaries();
    }

    private void stubSalaries(Salary... salaries) {
        when(this.salaryRepository.findByUserIdOrderByValidFromDesc(anyLong())).thenReturn(List.of(salaries));
    }

    private Salary salary(String amount, String currencyId, LocalDate from, LocalDate to) {
        return Salary.builder()
                .amount(new BigDecimal(amount))
                .basis(SalaryBasis.MONTHLY)
                .currency(new Currency(currencyId, currencyId))
                .validFrom(from)
                .validTo(to)
                .dependents(0)
                .build();
    }

    private PensionProjectionDTO.PensionProjectionDTOBuilder plan() {
        return PensionProjectionDTO.builder()
                .currentAge(35)
                .stopWorkingAge(65)
                .retirementAge(65)
                .yearsAlreadyWorked(10)
                .currentGrossMonthlyOverride(GROSS)
                .realWageGrowth(BigDecimal.ZERO)
                .inflation(new BigDecimal("3"))
                .degresszio(DegressioMode.IGNORED)
                .includeThirteenthMonth(false)
                .scenarios(List.of(this.flat()));
    }

    private PensionScenarioInputDTO flat() {
        return new PensionScenarioInputDTO("Frozen", PensionScenarioType.REAL_FLAT, null, null);
    }

    private PensionScenarioResultDTO only(PensionProjectionResultDTO result) {
        return result.getScenarios().getFirst();
    }

    @Test
    void project_keepsAFlatRealSalaryInTodaysMoney() {
        PensionProjectionResultDTO result = this.service.project(USER, this.plan().build());

        assertEquals(GROSS.doubleValue() * PENSION_NET,
                this.only(result).getPensionBaseMonthly().doubleValue(), TOLERANCE);
    }

    @Test
    void project_valorizesEachYearByTheRealWageGrowthUpToTheYearBeforeRetirement() {
        PensionProjectionResultDTO result = this.service.project(USER, this.plan()
                .currentAge(63).stopWorkingAge(65).retirementAge(65).yearsAlreadyWorked(0)
                .realWageGrowth(new BigDecimal("2"))
                .build());

        double net = GROSS.doubleValue() * PENSION_NET;
        assertEquals((net * 1.02 + net) / 2,
                this.only(result).getPensionBaseMonthly().doubleValue(), TOLERANCE);
    }

    @Test
    void project_scoresTwentyFiveServiceYearsAtSixtyThreePercent() {
        PensionProjectionResultDTO result = this.service.project(USER, this.plan()
                .currentAge(35).yearsAlreadyWorked(10).stopWorkingAge(50).retirementAge(65)
                .build());

        assertEquals(25, result.getServiceYears());
        assertEquals(63, result.getScalePct().doubleValue(), TOLERANCE);
        assertTrue(result.isEligible());
        assertFalse(result.isPartialPension());
    }

    @Test
    void project_reportsTheYearsBetweenStoppingWorkAndTheStateRetirementAge() {
        PensionProjectionResultDTO result = this.service.project(USER, this.plan()
                .currentAge(35).stopWorkingAge(50).retirementAge(65)
                .build());

        assertEquals(15, result.getGapYears());
        assertEquals(THIS_YEAR + 30, result.getRetirementYear());
    }

    @Test
    void project_paysNothingBelowFifteenServiceYears() {
        PensionProjectionResultDTO result = this.service.project(USER, this.plan()
                .currentAge(35).yearsAlreadyWorked(0).stopWorkingAge(49)
                .build());

        assertEquals(14, result.getServiceYears());
        assertFalse(result.isEligible());
        assertEquals(0, this.only(result).getMonthlyPension().doubleValue(), TOLERANCE);
    }

    @Test
    void project_givesAPartialPensionNoMinimumBetweenFifteenAndTwentyYears() {
        PensionProjectionResultDTO result = this.service.project(USER, this.plan()
                .currentAge(35).yearsAlreadyWorked(0).stopWorkingAge(52)
                .currentGrossMonthlyOverride(new BigDecimal("100"))
                .build());

        assertEquals(17, result.getServiceYears());
        assertTrue(result.isPartialPension());
        assertFalse(this.only(result).isMinimumApplied());
        assertTrue(this.only(result).getMonthlyPension().doubleValue() > 0);
        assertTrue(this.only(result).getMonthlyPension().doubleValue() < 28_500);
    }

    @Test
    void project_liftsAFullPensionToTheStatutoryMinimum() {
        PensionProjectionResultDTO result = this.service.project(USER, this.plan()
                .currentAge(35).yearsAlreadyWorked(0).stopWorkingAge(55)
                .currentGrossMonthlyOverride(new BigDecimal("100"))
                .build());

        assertEquals(20, result.getServiceYears());
        assertTrue(this.only(result).isMinimumApplied());
        assertEquals(28_500, this.only(result).getMonthlyPension().doubleValue(), TOLERANCE);
    }

    @Test
    void project_cutsTheBaseHardestWhenTheDegressioThresholdsStayFrozen() {
        PensionProjectionDTO.PensionProjectionDTOBuilder rich =
                this.plan().currentGrossMonthlyOverride(new BigDecimal("1500000"));

        double ignored = this.only(this.service.project(USER,
                rich.degresszio(DegressioMode.IGNORED).build())).getDegressedBaseMonthly().doubleValue();
        double indexed = this.only(this.service.project(USER,
                rich.degresszio(DegressioMode.INDEXED).build())).getDegressedBaseMonthly().doubleValue();
        double frozen = this.only(this.service.project(USER,
                rich.degresszio(DegressioMode.FROZEN).build())).getDegressedBaseMonthly().doubleValue();

        assertTrue(ignored > indexed, "ignoring degresszio must be the most generous");
        assertTrue(indexed > frozen, "frozen thresholds must bite harder than wage-indexed ones");
    }

    @Test
    void project_appliesTheDegressioBandsToTheNetAverage() {
        PensionProjectionResultDTO result = this.service.project(USER, this.plan()
                .currentGrossMonthlyOverride(new BigDecimal("1500000"))
                .degresszio(DegressioMode.INDEXED)
                .realWageGrowth(BigDecimal.ZERO)
                .build());

        double base = this.only(result).getPensionBaseMonthly().doubleValue();
        double expected = 372_000 + (421_000 - 372_000) * 0.9 + (base - 421_000) * 0.8;

        assertEquals(expected, this.only(result).getDegressedBaseMonthly().doubleValue(), TOLERANCE);
    }

    @Test
    void project_leavesABaseBelowTheLowerThresholdAlone() {
        PensionProjectionResultDTO result = this.service.project(USER, this.plan()
                .currentGrossMonthlyOverride(new BigDecimal("300000"))
                .degresszio(DegressioMode.INDEXED)
                .realWageGrowth(BigDecimal.ZERO)
                .build());

        assertEquals(this.only(result).getPensionBaseMonthly().doubleValue(),
                this.only(result).getDegressedBaseMonthly().doubleValue(), TOLERANCE);
    }

    @Test
    void project_erodesASalaryThatIsLockedInNominalTerms() {
        PensionProjectionDTO input = this.plan()
                .scenarios(List.of(this.flat(),
                        new PensionScenarioInputDTO("Locked", PensionScenarioType.NOMINAL_LOCK, null, null)))
                .build();

        PensionProjectionResultDTO result = this.service.project(USER, input);

        assertTrue(result.getScenarios().get(1).getPensionBaseMonthly().doubleValue()
                < result.getScenarios().getFirst().getPensionBaseMonthly().doubleValue());
    }

    @Test
    void project_liftsTheBaseWhenPayOutgrowsInflation() {
        PensionProjectionDTO input = this.plan()
                .scenarios(List.of(this.flat(), new PensionScenarioInputDTO("Rising",
                        PensionScenarioType.REAL_GROWTH, new BigDecimal("2"), null)))
                .build();

        PensionProjectionResultDTO result = this.service.project(USER, input);

        assertTrue(result.getScenarios().get(1).getPensionBaseMonthly().doubleValue()
                > result.getScenarios().getFirst().getPensionBaseMonthly().doubleValue());
    }

    @Test
    void project_followsTheStepsOfACustomSalaryPath() {
        PensionProjectionDTO input = this.plan()
                .currentAge(35).stopWorkingAge(50)
                .scenarios(List.of(new PensionScenarioInputDTO("Stepped", PensionScenarioType.STEPS, null,
                        List.of(new PensionStepDTO(40, new BigDecimal("5")),
                                new PensionStepDTO(50, BigDecimal.ZERO)))))
                .build();

        PensionProjectionResultDTO result = this.service.project(USER, input);
        List<PensionStopAgePointDTO> curve = this.only(result).getByStopAge();

        assertEquals(GROSS.doubleValue() * Math.pow(1.05, 5),
                this.only(result).getFinalGrossMonthly().doubleValue(), 1.0);
        assertFalse(curve.isEmpty());
    }

    @Test
    void project_addsTheThirteenthMonthToTheAnnualPension() {
        PensionProjectionResultDTO twelve = this.service.project(USER,
                this.plan().includeThirteenthMonth(false).build());
        PensionProjectionResultDTO thirteen = this.service.project(USER,
                this.plan().includeThirteenthMonth(true).build());

        assertEquals(this.only(twelve).getMonthlyPension().doubleValue() * 12,
                this.only(twelve).getAnnualPension().doubleValue(), TOLERANCE);
        assertEquals(this.only(thirteen).getMonthlyPension().doubleValue() * 13,
                this.only(thirteen).getAnnualPension().doubleValue(), TOLERANCE);
    }

    @Test
    void project_walksEveryStopAgeUpToTheRetirementAge() {
        PensionProjectionResultDTO result = this.service.project(USER, this.plan()
                .currentAge(35).retirementAge(65).build());
        List<PensionStopAgePointDTO> curve = this.only(result).getByStopAge();

        assertEquals(31, curve.size());
        assertEquals(35, curve.getFirst().getStopAge());
        assertEquals(65, curve.getLast().getStopAge());
    }

    @Test
    void project_paysMoreForEachExtraYearOnAFlatRealSalary() {
        List<PensionStopAgePointDTO> curve = this.only(this.service.project(USER, this.plan()
                .currentAge(35).yearsAlreadyWorked(10).retirementAge(65).build())).getByStopAge();

        for (int index = 1; index < curve.size(); index++) {
            assertTrue(curve.get(index).getMonthlyPension().doubleValue()
                            >= curve.get(index - 1).getMonthlyPension().doubleValue(),
                    "stopping at " + curve.get(index).getStopAge() + " must not pay less");
        }
    }

    @Test
    void project_bringsPastForintSalariesBackToTodaysMoney() {
        this.stubSalaries(this.salary("500000", "HUF", LocalDate.of(THIS_YEAR - 2, 1, 1), null));

        PensionProjectionResultDTO result = this.service.project(USER, this.plan()
                .currentAge(35).yearsAlreadyWorked(2).stopWorkingAge(36)
                .currentGrossMonthlyOverride(GROSS)
                .build());

        double expected = (500_000 * Math.pow(1.03, 2) * PENSION_NET
                + 500_000 * 1.03 * PENSION_NET
                + GROSS.doubleValue() * PENSION_NET) / 3;

        assertEquals(expected, this.only(result).getPensionBaseMonthly().doubleValue(), TOLERANCE);
    }

    @Test
    void project_warnsAboutSalariesThatAreNotInForint() {
        this.stubSalaries(this.salary("4000", "EUR", LocalDate.of(THIS_YEAR - 2, 1, 1), null));

        PensionProjectionResultDTO result = this.service.project(USER, this.plan().build());

        assertEquals(1, result.getWarnings().size());
        assertTrue(result.getWarnings().getFirst().contains("EUR"));
    }

    @Test
    void project_fallsBackToTheCurrentForintSalaryWhenNoGrossIsGiven() {
        this.stubSalaries(this.salary("750000", "HUF", LocalDate.of(THIS_YEAR - 1, 1, 1), null));

        PensionProjectionResultDTO result = this.service.project(USER, this.plan()
                .currentGrossMonthlyOverride(null).build());

        assertEquals(750_000, result.getCurrentGrossMonthly().doubleValue(), TOLERANCE);
    }

    @Test
    void project_reportsTheReplacementRateAgainstTheFinalPayrollNet() {
        PensionProjectionResultDTO result = this.service.project(USER, this.plan()
                .currentAge(35).yearsAlreadyWorked(10).stopWorkingAge(65).build());

        PensionScenarioResultDTO scenario = this.only(result);
        assertEquals(GROSS.doubleValue() * (1 - 0.185 - 0.15),
                scenario.getFinalNetMonthly().doubleValue(), TOLERANCE);
        assertEquals(scenario.getMonthlyPension().doubleValue() / scenario.getFinalNetMonthly().doubleValue() * 100,
                scenario.getReplacementRatePct().doubleValue(), TOLERANCE);
    }

    @Test
    void project_rejectsStoppingWorkBeforeToday() {
        PensionProjectionDTO input = this.plan().currentAge(40).stopWorkingAge(35).build();

        assertThrows(ValidationException.class, () -> this.service.project(USER, input));
    }

    @Test
    void project_rejectsAPensionThatWouldStartBeforeWorkStops() {
        PensionProjectionDTO input = this.plan().stopWorkingAge(60).retirementAge(55).build();

        assertThrows(ValidationException.class, () -> this.service.project(USER, input));
    }

    @Test
    void project_rejectsAStepScenarioWithNoSteps() {
        PensionProjectionDTO input = this.plan()
                .scenarios(List.of(new PensionScenarioInputDTO("Stepped", PensionScenarioType.STEPS, null, null)))
                .build();

        assertThrows(ValidationException.class, () -> this.service.project(USER, input));
    }

    @Test
    void project_rejectsTwoStepsEndingAtTheSameAge() {
        PensionProjectionDTO input = this.plan()
                .scenarios(List.of(new PensionScenarioInputDTO("Stepped", PensionScenarioType.STEPS, null,
                        List.of(new PensionStepDTO(45, new BigDecimal("3")),
                                new PensionStepDTO(45, new BigDecimal("1"))))))
                .build();

        assertThrows(ValidationException.class, () -> this.service.project(USER, input));
    }

    @Test
    void project_rejectsWhenThereIsNoForintSalaryAndNoGrossGiven() {
        this.stubSalaries(this.salary("4000", "EUR", LocalDate.of(THIS_YEAR - 2, 1, 1), null));
        PensionProjectionDTO input = this.plan().currentGrossMonthlyOverride(null).build();

        assertThrows(ValidationException.class, () -> this.service.project(USER, input));
    }

    @Test
    void getCSV_writesARowForEveryYearOfEveryScenario() {
        StringWriter writer = new StringWriter();

        this.service.getCSV(USER, this.plan().currentAge(60).stopWorkingAge(65).retirementAge(65)
                .yearsAlreadyWorked(0).build(), writer);
        String csv = writer.toString();

        assertTrue(csv.contains("Valorized net monthly"));
        assertTrue(csv.contains("Frozen"));
        assertEquals(6, csv.lines().count());
    }

    @Test
    void project_marksTheYearsAfterStoppingAsNotWorking() {
        PensionProjectionResultDTO result = this.service.project(USER, this.plan()
                .currentAge(60).stopWorkingAge(62).retirementAge(65).yearsAlreadyWorked(0).build());

        List<PensionYearDTO> timeline = this.only(result).getTimeline();

        assertEquals(5, timeline.size());
        assertTrue(timeline.getFirst().isWorking());
        assertFalse(timeline.getLast().isWorking());
        assertEquals(0, timeline.getLast().getValorizedNetMonthly().doubleValue(), TOLERANCE);
    }

    @Test
    void project_weightsOverlappingSalaryRecordsByTheDaysEachCovers() {
        int year = THIS_YEAR - 1;
        this.stubSalaries(
                this.salary("1200000", "HUF", LocalDate.of(year, 7, 1), null),
                this.salary("600000", "HUF", LocalDate.of(year, 1, 1), LocalDate.of(year, 6, 30)));

        PensionProjectionResultDTO result = this.service.project(USER, this.plan()
                .currentAge(35).yearsAlreadyWorked(1).stopWorkingAge(35)
                .currentGrossMonthlyOverride(GROSS)
                .build());

        double firstDays = ChronoUnit.DAYS.between(
                LocalDate.of(year, 1, 1), LocalDate.of(year, 6, 30)) + 1.0;
        double secondDays = ChronoUnit.DAYS.between(
                LocalDate.of(year, 7, 1), LocalDate.of(year, 12, 31)) + 1.0;
        double weighted = (firstDays * 600_000 + secondDays * 1_200_000) / (firstDays + secondDays);

        assertEquals(weighted * 1.03 * PENSION_NET,
                this.only(result).getPensionBaseMonthly().doubleValue(), TOLERANCE);
        assertTrue(this.only(result).getPensionBaseMonthly().doubleValue()
                        < 1_200_000 * 1.03 * PENSION_NET,
                "the later record must not take the whole year");
    }

    @Test
    void project_countsAPartialYearLessThanAFullOne() {
        int last = THIS_YEAR - 1;
        int earlier = THIS_YEAR - 2;

        this.stubSalaries(
                this.salary("1200000", "HUF", LocalDate.of(last, 10, 1), null),
                this.salary("600000", "HUF", LocalDate.of(earlier, 1, 1),
                        LocalDate.of(earlier, 12, 31)));
        double partial = this.only(this.service.project(USER, this.plan()
                .currentAge(35).yearsAlreadyWorked(2).stopWorkingAge(35).build()))
                .getPensionBaseMonthly().doubleValue();

        this.stubSalaries(
                this.salary("1200000", "HUF", LocalDate.of(last, 1, 1), null),
                this.salary("600000", "HUF", LocalDate.of(earlier, 1, 1),
                        LocalDate.of(earlier, 12, 31)));
        double full = this.only(this.service.project(USER, this.plan()
                .currentAge(35).yearsAlreadyWorked(2).stopWorkingAge(35).build()))
                .getPensionBaseMonthly().doubleValue();

        assertTrue(partial < full, "a quarter of a year must not weigh as much as a whole one");
    }

    @Test
    void project_appliesDegressioAsTheLawStandsWhenNoModeIsChosen() {
        PensionProjectionResultDTO result = this.service.project(USER,
                this.plan().degresszio(null).build());

        assertEquals(DegressioMode.FROZEN, result.getDegresszio());
    }

    @Test
    void project_showsHowMuchTheWageGrowthAssumptionIsWorth() {
        PensionProjectionResultDTO result = this.service.project(USER, this.plan()
                .realWageGrowth(new BigDecimal("1.5")).build());
        List<PensionSensitivityPointDTO> rows = result.getWageGrowthSensitivity();

        assertEquals(List.of(0.0, 1.0, 1.5, 2.0, 3.0), rows.stream()
                .map(row -> row.getRealWageGrowth().doubleValue()).toList());
        assertEquals(1, rows.stream().filter(PensionSensitivityPointDTO::isSelected).count());
        assertEquals(1.5, rows.stream().filter(PensionSensitivityPointDTO::isSelected)
                .findFirst().orElseThrow().getRealWageGrowth().doubleValue(), TOLERANCE);

        for (int index = 1; index < rows.size(); index++) {
            assertTrue(rows.get(index).getMonthlyPension().doubleValue()
                            > rows.get(index - 1).getMonthlyPension().doubleValue(),
                    "faster wage growth must lift the pension");
        }
    }

    @Test
    void project_reportsTheUpliftValorizationAddsOnTopOfTodaysWages() {
        PensionProjectionResultDTO result = this.service.project(USER, this.plan()
                .currentAge(35).retirementAge(65).realWageGrowth(new BigDecimal("2")).build());

        assertEquals(Math.pow(1.02, 29), result.getValorizationUplift().doubleValue(), TOLERANCE);
        assertEquals(2.0, result.getRealWageGrowth().doubleValue(), TOLERANCE);
    }

    @Test
    void project_leavesNoUpliftWhenWagesOnlyKeepPaceWithPrices() {
        PensionProjectionResultDTO result = this.service.project(USER,
                this.plan().realWageGrowth(BigDecimal.ZERO).build());

        assertEquals(1.0, result.getValorizationUplift().doubleValue(), TOLERANCE);
        assertEquals(GROSS.doubleValue() * PENSION_NET,
                this.only(result).getPensionBaseMonthly().doubleValue(), TOLERANCE);
    }

    @Test
    void project_comparesTheResultWithWhatAnAverageEarnerWouldDraw() {
        PensionProjectionResultDTO result = this.service.project(USER, this.plan()
                .currentAge(35).yearsAlreadyWorked(10).stopWorkingAge(50)
                .realWageGrowth(BigDecimal.ZERO)
                .degresszio(DegressioMode.IGNORED)
                .build());

        double averagePension = AVERAGE_GROSS * PENSION_NET * 0.63;

        assertEquals(25, result.getServiceYears());
        assertEquals(averagePension, result.getNationalAveragePension().doubleValue(), TOLERANCE);
        assertEquals(GROSS.doubleValue() / AVERAGE_GROSS,
                result.getSalaryMultipleOfNationalAverage().doubleValue(), TOLERANCE);
        assertEquals(this.only(result).getMonthlyPension().doubleValue() / averagePension,
                result.getNationalAverageMultiple().doubleValue(), 0.01);
    }

    @Test
    void project_liftsTheAverageEarnerByTheSameWageGrowth() {
        double flat = this.service.project(USER, this.plan().realWageGrowth(BigDecimal.ZERO).build())
                .getNationalAveragePension().doubleValue();
        double growing = this.service.project(USER, this.plan().realWageGrowth(new BigDecimal("2")).build())
                .getNationalAveragePension().doubleValue();

        assertTrue(growing > flat, "the benchmark must move with the same assumption as the result");
    }
}
