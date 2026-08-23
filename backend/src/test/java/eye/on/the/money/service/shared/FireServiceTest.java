package eye.on.the.money.service.shared;

import eye.on.the.money.dto.in.FireProjectionDTO;
import eye.on.the.money.dto.out.FireProjectionResultDTO;
import eye.on.the.money.dto.out.FireYearDTO;
import eye.on.the.money.dto.out.NetWorthDTO;
import eye.on.the.money.exception.FireException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.StringWriter;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FireServiceTest {

    private static final String USER = "user@eotm.com";
    private static final double TOLERANCE = 0.01;

    @Mock
    private NetWorthService netWorthService;

    private FireService fireService;

    @BeforeEach
    void setUp() {
        this.fireService = new FireService(this.netWorthService);
        this.stubPortfolio(0);
    }

    private void stubPortfolio(double worth) {
        when(this.netWorthService.getNetWorth(anyString(), any(), anyBoolean())).thenReturn(NetWorthDTO.builder()
                .currency("HUF")
                .totalWorth(BigDecimal.valueOf(worth))
                .unconvertedCurrencies(List.of())
                .build());
    }

    /** A plan that does nothing on its own, so each test can vary just the part it is about. */
    private FireProjectionDTO.FireProjectionDTOBuilder plan() {
        return FireProjectionDTO.builder()
                .currency("HUF")
                .otherAssets(BigDecimal.ZERO)
                .monthlyContribution(BigDecimal.ZERO)
                .annualContributionIncrease(BigDecimal.ZERO)
                .annualReturn(BigDecimal.ZERO)
                .inflation(BigDecimal.ZERO)
                .annualSpending(BigDecimal.valueOf(1_000_000))
                .withdrawalRate(BigDecimal.valueOf(4))
                .currentAge(30)
                .lifeExpectancy(90);
    }

    private FireYearDTO yearOf(FireProjectionResultDTO result, int year) {
        return result.getTimeline().stream()
                .filter(point -> point.getYear() == year).findFirst().orElseThrow();
    }

    @Test
    void project_addsOtherAssetsToTheLivePortfolio() {
        this.stubPortfolio(5_000_000);

        FireProjectionResultDTO result = this.fireService.project(USER,
                this.plan().otherAssets(BigDecimal.valueOf(3_000_000)).build());

        assertEquals(0, result.getPortfolioValue().compareTo(new BigDecimal("5000000.00")));
        assertEquals(0, result.getOtherAssets().compareTo(new BigDecimal("3000000.00")));
        assertEquals(0, result.getStartingValue().compareTo(new BigDecimal("8000000.00")));
    }

    @Test
    void project_derivesTheFireNumberFromSpendingAndWithdrawalRate() {
        FireProjectionResultDTO result = this.fireService.project(USER,
                this.plan().annualSpending(BigDecimal.valueOf(4_000_000))
                        .withdrawalRate(BigDecimal.valueOf(4)).build());

        // 4,000,000 / 4% = 100,000,000
        assertEquals(0, result.getFireNumber().compareTo(new BigDecimal("100000000.00")));
        assertFalse(result.isFireNumberOverridden());
    }

    @Test
    void project_backSolvesSpendingWhenTheFireNumberIsGivenDirectly() {
        FireProjectionResultDTO result = this.fireService.project(USER,
                this.plan().annualSpending(null).fireNumber(BigDecimal.valueOf(50_000_000))
                        .withdrawalRate(BigDecimal.valueOf(4)).build());

        assertEquals(0, result.getFireNumber().compareTo(new BigDecimal("50000000.00")));
        assertEquals(0, result.getAnnualSpending().compareTo(new BigDecimal("2000000.00")));
        assertTrue(result.isFireNumberOverridden());
    }

    @Test
    void project_prefersTheSuppliedFireNumberOverTheOneSpendingImplies() {
        FireProjectionResultDTO result = this.fireService.project(USER,
                this.plan().annualSpending(BigDecimal.valueOf(4_000_000))
                        .fireNumber(BigDecimal.valueOf(20_000_000)).build());

        assertEquals(0, result.getFireNumber().compareTo(new BigDecimal("20000000.00")));
    }

    @Test
    void project_rejectsAPlanWithNoTargetAtAll() {
        assertThrows(FireException.class, () -> this.fireService.project(USER,
                this.plan().annualSpending(null).fireNumber(null).build()));
    }

    @Test
    void project_compoundsMonthlyToTheStatedAnnualReturn() {
        this.stubPortfolio(1_000_000);

        FireProjectionResultDTO result = this.fireService.project(USER,
                this.plan().annualReturn(BigDecimal.valueOf(10)).build());

        // Twelve monthly steps of (1.10)^(1/12) come back to exactly 10% over the year.
        assertEquals(1_100_000, this.yearOf(result, 1).getBalance().doubleValue(), 1.0);
        assertEquals(1_210_000, this.yearOf(result, 2).getBalance().doubleValue(), 1.0);
    }

    @Test
    void project_reportsWhatWasEarnedAtTheAnnualReturn() {
        this.stubPortfolio(1_000_000);

        FireProjectionResultDTO result = this.fireService.project(USER,
                this.plan().annualReturn(BigDecimal.valueOf(10)).build());

        assertEquals(0, this.yearOf(result, 0).getGrowth().signum());
        assertEquals(100_000, this.yearOf(result, 1).getGrowth().doubleValue(), 1.0);
        assertEquals(110_000, this.yearOf(result, 2).getGrowth().doubleValue(), 1.0);
    }

    @Test
    void project_reportsNothingEarnedWhenThereIsNoReturn() {
        this.stubPortfolio(1_000_000);

        FireProjectionResultDTO result = this.fireService.project(USER, this.plan().build());

        assertEquals(0, this.yearOf(result, 1).getGrowth().signum());
    }

    @Test
    void project_reportsALossWhenTheReturnIsNegative() {
        this.stubPortfolio(1_000_000);

        FireProjectionResultDTO result = this.fireService.project(USER,
                this.plan().annualReturn(BigDecimal.valueOf(-10)).build());

        assertEquals(-100_000, this.yearOf(result, 1).getGrowth().doubleValue(), 1.0);
    }

    @Test
    void project_growthAccountsForTheRestOfTheYearsMovement() {
        this.stubPortfolio(1_000_000);

        FireProjectionResultDTO result = this.fireService.project(USER,
                this.plan().annualReturn(BigDecimal.valueOf(10))
                        .monthlyContribution(BigDecimal.valueOf(100_000)).build());

        FireYearDTO first = this.yearOf(result, 1);
        assertEquals(first.getBalance().doubleValue(),
                this.yearOf(result, 0).getBalance().doubleValue()
                        + first.getContributions().doubleValue()
                        + first.getGrowth().doubleValue(), TOLERANCE);
    }

    @Test
    void project_keepsReportingGrowthOnWhatIsLeftInDrawdown() {
        this.stubPortfolio(100_000_000);

        FireProjectionResultDTO result = this.fireService.project(USER,
                this.plan().annualSpending(BigDecimal.valueOf(4_000_000))
                        .annualReturn(BigDecimal.valueOf(10)).retirementAge(30).build());

        FireYearDTO firstRetired = this.yearOf(result, 1);
        assertEquals("DRAWDOWN", firstRetired.getPhase());
        assertEquals(firstRetired.getBalance().doubleValue(),
                100_000_000 + firstRetired.getGrowth().doubleValue()
                        - firstRetired.getWithdrawals().doubleValue(), TOLERANCE);
    }

    @Test
    void project_addsContributionsEveryMonth() {
        FireProjectionResultDTO result = this.fireService.project(USER,
                this.plan().monthlyContribution(BigDecimal.valueOf(100_000)).build());

        assertEquals(1_200_000, this.yearOf(result, 1).getBalance().doubleValue(), TOLERANCE);
        assertEquals(1_200_000, this.yearOf(result, 1).getContributions().doubleValue(), TOLERANCE);
    }

    @Test
    void project_raisesTheContributionOncePerYear() {
        FireProjectionResultDTO result = this.fireService.project(USER,
                this.plan().monthlyContribution(BigDecimal.valueOf(100_000))
                        .annualContributionIncrease(BigDecimal.valueOf(10)).build());

        // The first year is paid at the starting rate; the rise applies from the second.
        assertEquals(1_200_000, this.yearOf(result, 1).getContributions().doubleValue(), TOLERANCE);
        assertEquals(1_320_000, this.yearOf(result, 2).getContributions().doubleValue(), TOLERANCE);
    }

    @Test
    void project_discountsTheBalanceBackToTodaysMoney() {
        this.stubPortfolio(1_000_000);

        FireProjectionResultDTO result = this.fireService.project(USER,
                this.plan().inflation(BigDecimal.valueOf(10)).build());

        // The pot does not move, but ten years of 10% inflation leaves it worth 1,000,000 / 1.1^10.
        assertEquals(1_000_000, this.yearOf(result, 10).getBalance().doubleValue(), TOLERANCE);
        assertEquals(385_543.29, this.yearOf(result, 10).getRealBalance().doubleValue(), 1.0);
    }

    /**
     * A target worked out from spending inherits that spending's units, and spending is given in today's
     * money. Measuring it against the nominal balance would call financial independence years early.
     */
    @Test
    void project_measuresADerivedTargetAgainstTodaysMoney() {
        this.stubPortfolio(100_000_000);

        // Growth and inflation cancel, so the pot never gains real ground on the 150,000,000 that 6,000,000
        // a year at 4% needs. The nominal balance does clear it, in year five.
        FireProjectionResultDTO result = this.fireService.project(USER,
                this.plan().annualSpending(BigDecimal.valueOf(6_000_000))
                        .annualReturn(BigDecimal.valueOf(10)).inflation(BigDecimal.valueOf(10)).build());

        assertEquals(0, result.getFireNumber().compareTo(new BigDecimal("150000000.00")));
        assertTrue(result.isFireNumberInTodaysMoney());
        assertTrue(this.yearOf(result, 5).getBalance().doubleValue() > 150_000_000);
        assertEquals(100_000_000, this.yearOf(result, 5).getRealBalance().doubleValue(), 1.0);
        assertFalse(result.isFiReached());
        assertNull(result.getFiYear());
    }

    @Test
    void project_reachesADerivedTargetOnceTheRealBalanceClearsIt() {
        this.stubPortfolio(100_000_000);

        // A real return of 10% a year takes 100m past the 150m target during the fifth year.
        FireProjectionResultDTO result = this.fireService.project(USER,
                this.plan().annualSpending(BigDecimal.valueOf(6_000_000))
                        .annualReturn(BigDecimal.valueOf(10)).inflation(BigDecimal.ZERO).build());

        assertTrue(result.isFiReached());
        assertEquals(5, result.getFiYear());
        assertEquals(35, result.getFiAge());
    }

    /**
     * A target typed in directly is a plain amount of money: asking for 150m means wanting to see 150m in
     * the account, whatever it is worth by then.
     */
    @Test
    void project_measuresATypedTargetAgainstTheBalanceItself() {
        this.stubPortfolio(100_000_000);

        FireProjectionResultDTO result = this.fireService.project(USER,
                this.plan().annualSpending(null).fireNumber(BigDecimal.valueOf(150_000_000))
                        .annualReturn(BigDecimal.valueOf(10)).inflation(BigDecimal.valueOf(10)).build());

        // The real balance never moves off 100m, so only the nominal reading can reach the target.
        assertFalse(result.isFireNumberInTodaysMoney());
        assertEquals(100_000_000, this.yearOf(result, 5).getRealBalance().doubleValue(), 1.0);
        assertTrue(result.isFiReached());
        assertEquals(5, result.getFiYear());
    }

    @Test
    void project_reportsProgressTowardsATypedTargetOnTheNominalBalance() {
        this.stubPortfolio(75_000_000);

        FireProjectionResultDTO result = this.fireService.project(USER,
                this.plan().annualSpending(null).fireNumber(BigDecimal.valueOf(150_000_000))
                        .inflation(BigDecimal.valueOf(10)).build());

        // 75m of a 150m target, unaffected by the ten years of inflation between here and there.
        assertEquals(50.0, this.yearOf(result, 10).getPctOfFireNumber().doubleValue(), TOLERANCE);
    }

    /**
     * The drawdown works in today's money and inflates each year to the year it is spent in, so a typed
     * target has to be brought back to today first or the income would be inflated twice over.
     */
    @Test
    void project_convertsATypedTargetsIncomeBackToTodaysMoney() {
        this.stubPortfolio(100_000_000);

        FireProjectionResultDTO result = this.fireService.project(USER,
                this.plan().annualSpending(null).fireNumber(BigDecimal.valueOf(150_000_000))
                        .annualReturn(BigDecimal.valueOf(10)).inflation(BigDecimal.valueOf(10)).build());

        // 150m is reached in year five, where 4% of it is 6,000,000 of that year's money. Discounted back
        // over those five years that is 6,000,000 / 1.1^5.
        assertEquals(5, result.getRetirementYear());
        assertEquals(3_725_528.05, result.getAnnualSpending().doubleValue(), 1.0);
        assertEquals(6_600_000, result.getFirstYearWithdrawal().doubleValue(), 1.0);
    }

    @Test
    void project_drawsTheReportedFirstYearAmountInTheFirstYearOfRetirement() {
        this.stubPortfolio(100_000_000);

        FireProjectionResultDTO result = this.fireService.project(USER,
                this.plan().annualSpending(null).fireNumber(BigDecimal.valueOf(150_000_000))
                        .annualReturn(BigDecimal.valueOf(10)).inflation(BigDecimal.valueOf(10)).build());

        FireYearDTO firstRetired = this.yearOf(result, result.getRetirementYear() + 1);
        assertEquals("DRAWDOWN", firstRetired.getPhase());
        assertEquals(result.getFirstYearWithdrawal().doubleValue(),
                firstRetired.getWithdrawals().doubleValue(), 1.0);
    }

    @Test
    void project_reportsWhenTheTargetIsNeverReached() {
        FireProjectionResultDTO result = this.fireService.project(USER,
                this.plan().annualSpending(BigDecimal.valueOf(4_000_000)).build());

        assertFalse(result.isFiReached());
        assertNull(result.getFiYear());
        assertNull(result.getRetirementYear());
    }

    @Test
    void project_retiresAsSoonAsTheTargetIsReachedWhenNoAgeIsSet() {
        this.stubPortfolio(100_000_000);

        FireProjectionResultDTO result = this.fireService.project(USER,
                this.plan().annualSpending(BigDecimal.valueOf(4_000_000)).build());

        assertEquals(result.getFiYear(), result.getRetirementYear());
    }

    @Test
    void project_honoursAStatedRetirementAgeOverTheTarget() {
        this.stubPortfolio(100_000_000);

        FireProjectionResultDTO result = this.fireService.project(USER,
                this.plan().annualSpending(BigDecimal.valueOf(4_000_000)).retirementAge(50).build());

        assertEquals(50, result.getRetirementAge());
        assertEquals(20, result.getRetirementYear());
        assertEquals("ACCUMULATION", this.yearOf(result, 20).getPhase());
        assertEquals("DRAWDOWN", this.yearOf(result, 21).getPhase());
    }

    @Test
    void project_stopsContributingAndStartsWithdrawingAtRetirement() {
        this.stubPortfolio(100_000_000);

        FireProjectionResultDTO result = this.fireService.project(USER,
                this.plan().monthlyContribution(BigDecimal.valueOf(100_000)).retirementAge(40).build());

        FireYearDTO firstRetiredYear = this.yearOf(result, 11);
        assertEquals(0, firstRetiredYear.getContributions().signum());
        assertEquals(1_000_000, firstRetiredYear.getWithdrawals().doubleValue(), TOLERANCE);
    }

    /**
     * The pot stays invested after retirement rather than being counted down as cash, which is the whole
     * premise of a safe withdrawal rate.
     */
    @Test
    void project_keepsGrowingThePotThroughRetirement() {
        this.stubPortfolio(100_000_000);

        FireProjectionResultDTO result = this.fireService.project(USER,
                this.plan().annualSpending(BigDecimal.valueOf(4_000_000))
                        .annualReturn(BigDecimal.valueOf(10)).retirementAge(30).build());

        FireYearDTO firstRetired = this.yearOf(result, 1);
        assertEquals("DRAWDOWN", firstRetired.getPhase());
        assertEquals(4_000_000, firstRetired.getWithdrawals().doubleValue(), TOLERANCE);
        // A 10% return more than covers 4,000,000 of withdrawals, so the pot ends the year larger.
        assertTrue(firstRetired.getBalance().doubleValue() > 100_000_000);
    }

    @Test
    void project_shrinksThePotInRetirementWhenThereIsNoGrowthToCoverTheWithdrawals() {
        this.stubPortfolio(100_000_000);

        // The same plan with the return switched off: the pot loses exactly what is taken out of it.
        FireProjectionResultDTO result = this.fireService.project(USER,
                this.plan().annualSpending(BigDecimal.valueOf(4_000_000))
                        .annualReturn(BigDecimal.ZERO).retirementAge(30).build());

        assertEquals(96_000_000, this.yearOf(result, 1).getBalance().doubleValue(), TOLERANCE);
    }

    @Test
    void project_raisesTheWithdrawalWithInflation() {
        this.stubPortfolio(1_000_000_000);

        FireProjectionResultDTO result = this.fireService.project(USER,
                this.plan().annualSpending(BigDecimal.valueOf(1_000_000))
                        .inflation(BigDecimal.valueOf(10)).retirementAge(31).build());

        // Spending is set in today's money, so year two of the plan costs 1,000,000 * 1.1^2.
        assertEquals(1_210_000, this.yearOf(result, 2).getWithdrawals().doubleValue(), 1.0);
    }

    @Test
    void project_leavesThePotAloneUntilThePensionAgeIsReached() {
        this.stubPortfolio(100_000_000);

        // Retired at 30, pension from 40: the first ten years have to come from the pot alone.
        FireProjectionResultDTO result = this.fireService.project(USER,
                this.plan().annualSpending(BigDecimal.valueOf(4_800_000)).retirementAge(30)
                        .monthlyPension(BigDecimal.valueOf(300_000)).pensionAge(40).build());

        assertEquals(0, this.yearOf(result, 9).getPension().signum());
        assertEquals(4_800_000, this.yearOf(result, 9).getWithdrawals().doubleValue(), TOLERANCE);

        // 300,000 a month is 3,600,000 of the 4,800,000, leaving 1,200,000 to come out of the pot.
        assertEquals(3_600_000, this.yearOf(result, 10).getPension().doubleValue(), TOLERANCE);
        assertEquals(1_200_000, this.yearOf(result, 10).getWithdrawals().doubleValue(), TOLERANCE);
    }

    @Test
    void project_drawsNothingFromThePotWhenThePensionCoversTheSpending() {
        this.stubPortfolio(100_000_000);

        FireProjectionResultDTO result = this.fireService.project(USER,
                this.plan().annualSpending(BigDecimal.valueOf(4_800_000)).retirementAge(30)
                        .monthlyPension(BigDecimal.valueOf(400_000)).pensionAge(30).build());

        assertEquals(0, this.yearOf(result, 1).getWithdrawals().signum());
        // Nothing leaves the pot, so it sits at its starting value with no growth to move it.
        assertEquals(100_000_000, this.yearOf(result, 1).getBalance().doubleValue(), TOLERANCE);
    }

    @Test
    void project_addsAPensionLargerThanTheSpendingToThePot() {
        this.stubPortfolio(100_000_000);

        FireProjectionResultDTO result = this.fireService.project(USER,
                this.plan().annualSpending(BigDecimal.valueOf(4_800_000)).retirementAge(30)
                        .monthlyPension(BigDecimal.valueOf(600_000)).pensionAge(30).build());

        // 7,200,000 a year against 4,800,000 of spending; the extra 2,400,000 has nowhere else to go.
        assertEquals(0, this.yearOf(result, 1).getWithdrawals().signum());
        assertEquals(102_400_000, this.yearOf(result, 1).getBalance().doubleValue(), TOLERANCE);
    }

    @Test
    void project_addsAPensionArrivingDuringAccumulationToThePot() {
        this.stubPortfolio(10_000_000);

        // Still paying in at 30, but the pension is already being drawn.
        FireProjectionResultDTO result = this.fireService.project(USER,
                this.plan().monthlyContribution(BigDecimal.valueOf(100_000)).retirementAge(60)
                        .monthlyPension(BigDecimal.valueOf(200_000)).pensionAge(30).build());

        FireYearDTO first = this.yearOf(result, 1);
        assertEquals("ACCUMULATION", first.getPhase());
        assertEquals(1_200_000, first.getContributions().doubleValue(), TOLERANCE);
        assertEquals(2_400_000, first.getPension().doubleValue(), TOLERANCE);
        assertEquals(13_600_000, first.getBalance().doubleValue(), TOLERANCE);
    }

    @Test
    void project_raisesThePensionWithInflationLikeTheSpending() {
        this.stubPortfolio(1_000_000_000);

        FireProjectionResultDTO result = this.fireService.project(USER,
                this.plan().annualSpending(BigDecimal.valueOf(4_800_000)).retirementAge(30)
                        .monthlyPension(BigDecimal.valueOf(100_000)).pensionAge(30)
                        .inflation(BigDecimal.valueOf(10)).build());

        // Both are given in today's money, so year two pays 1,200,000 * 1.1^2 and the pot covers the rest.
        assertEquals(1_452_000, this.yearOf(result, 2).getPension().doubleValue(), 1.0);
        assertEquals(4_356_000, this.yearOf(result, 2).getWithdrawals().doubleValue(), 1.0);
    }

    @Test
    void project_makesThePotLastLongerWithAPension() {
        this.stubPortfolio(20_000_000);

        FireProjectionDTO.FireProjectionDTOBuilder plan = this.plan()
                .annualSpending(BigDecimal.valueOf(6_000_000)).retirementAge(30);

        FireProjectionResultDTO without = this.fireService.project(USER, plan.build());
        FireProjectionResultDTO with = this.fireService.project(USER,
                plan.monthlyPension(BigDecimal.valueOf(250_000)).pensionAge(32).build());

        // 20m at 6m a year is gone in four. Halving the draw from age 32 stretches it out.
        assertEquals(34, without.getDepletedAtAge());
        assertEquals(36, with.getDepletedAtAge());
    }

    @Test
    void project_ignoresAPensionWithNoAgeSet() {
        this.stubPortfolio(100_000_000);

        FireProjectionResultDTO result = this.fireService.project(USER,
                this.plan().annualSpending(BigDecimal.valueOf(4_800_000)).retirementAge(30)
                        .monthlyPension(BigDecimal.valueOf(300_000)).build());

        assertEquals(0, this.yearOf(result, 1).getPension().signum());
        assertEquals(4_800_000, this.yearOf(result, 1).getWithdrawals().doubleValue(), TOLERANCE);
    }

    @Test
    void project_treatsAPensionAgeAlreadyPassedAsInPaymentFromTheStart() {
        this.stubPortfolio(100_000_000);

        FireProjectionResultDTO result = this.fireService.project(USER,
                this.plan().annualSpending(BigDecimal.valueOf(4_800_000)).currentAge(70).retirementAge(70)
                        .lifeExpectancy(90).monthlyPension(BigDecimal.valueOf(250_000)).pensionAge(65).build());

        assertEquals(3_000_000, this.yearOf(result, 1).getPension().doubleValue(), TOLERANCE);
    }

    @Test
    void project_reportsTheAgeThePotRunsOut() {
        this.stubPortfolio(10_000_000);

        FireProjectionResultDTO result = this.fireService.project(USER,
                this.plan().annualSpending(BigDecimal.valueOf(5_000_000)).retirementAge(30).build());

        // Ten million with no growth, spent at five million a year, is gone in the second year.
        assertFalse(result.isLastsThroughRetirement());
        assertEquals(32, result.getDepletedAtAge());
        assertEquals(0, this.yearOf(result, 5).getBalance().signum());
    }

    @Test
    void project_neverReportsANegativeBalance() {
        this.stubPortfolio(1_000_000);

        FireProjectionResultDTO result = this.fireService.project(USER,
                this.plan().annualSpending(BigDecimal.valueOf(5_000_000)).retirementAge(30).build());

        assertTrue(result.getTimeline().stream().allMatch(point -> point.getBalance().signum() >= 0));
    }

    @Test
    void project_reportsThePotSurvivingToTheEndOfThePlan() {
        this.stubPortfolio(500_000_000);

        FireProjectionResultDTO result = this.fireService.project(USER,
                this.plan().annualSpending(BigDecimal.valueOf(1_000_000)).retirementAge(31).build());

        assertTrue(result.isLastsThroughRetirement());
        assertNull(result.getDepletedAtAge());
        assertEquals(90, result.getFinalAge());
    }

    @Test
    void project_runsTheTimelineFromTodayToTheEndOfThePlan() {
        FireProjectionResultDTO result = this.fireService.project(USER,
                this.plan().currentAge(30).lifeExpectancy(90).build());

        assertEquals(61, result.getTimeline().size());
        assertEquals(0, result.getTimeline().getFirst().getYear());
        assertEquals(30, result.getTimeline().getFirst().getAge());
        assertEquals(60, result.getTimeline().getLast().getYear());
        assertEquals(90, result.getTimeline().getLast().getAge());
    }

    @Test
    void project_addsTheYearTheTargetIsMetToTheMilestones() {
        this.stubPortfolio(100_000_000);

        // A 10% real return clears the 200m target in year 8, which is not one of the fixed horizons.
        FireProjectionResultDTO result = this.fireService.project(USER,
                this.plan().annualSpending(null).fireNumber(BigDecimal.valueOf(200_000_000))
                        .annualReturn(BigDecimal.valueOf(10)).inflation(BigDecimal.ZERO).build());

        assertEquals(8, result.getFiYear());
        assertEquals(List.of(1, 3, 5, 8, 10, 15, 20),
                result.getMilestones().stream().map(FireYearDTO::getYear).toList());
    }

    @Test
    void project_doesNotRepeatTheYearTheTargetIsMetWhenItIsAlreadyAMilestone() {
        this.stubPortfolio(100_000_000);

        FireProjectionResultDTO result = this.fireService.project(USER,
                this.plan().annualSpending(null).fireNumber(BigDecimal.valueOf(150_000_000))
                        .annualReturn(BigDecimal.valueOf(10)).inflation(BigDecimal.ZERO).build());

        assertEquals(5, result.getFiYear());
        assertEquals(List.of(1, 3, 5, 10, 15, 20),
                result.getMilestones().stream().map(FireYearDTO::getYear).toList());
    }

    @Test
    void project_picksTheSixMilestoneYears() {
        FireProjectionResultDTO result = this.fireService.project(USER, this.plan().build());

        assertEquals(List.of(1, 3, 5, 10, 15, 20),
                result.getMilestones().stream().map(FireYearDTO::getYear).toList());
    }

    @Test
    void project_startsTheTimelineAtTheStartingValue() {
        this.stubPortfolio(7_500_000);

        FireProjectionResultDTO result = this.fireService.project(USER, this.plan().build());

        FireYearDTO start = this.yearOf(result, 0);
        assertEquals(0, start.getBalance().compareTo(new BigDecimal("7500000.00")));
        assertEquals(0, start.getRealBalance().compareTo(new BigDecimal("7500000.00")));
    }

    @Test
    void project_reportsProgressTowardsTheTarget() {
        this.stubPortfolio(25_000_000);

        FireProjectionResultDTO result = this.fireService.project(USER,
                this.plan().annualSpending(BigDecimal.valueOf(4_000_000)).build());

        // 25,000,000 against a 100,000,000 target.
        assertEquals(25.0, this.yearOf(result, 0).getPctOfFireNumber().doubleValue(), TOLERANCE);
    }

    /** Total deflation leaves nothing to discount by, which would otherwise divide the real balance by zero. */
    @Test
    void project_rejectsInflationOfMinusOneHundredPercent() {
        assertThrows(FireException.class, () -> this.fireService.project(USER,
                this.plan().inflation(BigDecimal.valueOf(-100)).build()));
    }

    @Test
    void project_rejectsAPlanThatEndsBeforeItStarts() {
        assertThrows(FireException.class, () -> this.fireService.project(USER,
                this.plan().currentAge(60).lifeExpectancy(60).build()));
    }

    @Test
    void project_rejectsRetirementBeforeTheCurrentAge() {
        assertThrows(FireException.class, () -> this.fireService.project(USER,
                this.plan().currentAge(40).retirementAge(30).build()));
    }

    @Test
    void project_rejectsRetirementAfterTheEndOfThePlan() {
        assertThrows(FireException.class, () -> this.fireService.project(USER,
                this.plan().currentAge(30).retirementAge(95).lifeExpectancy(90).build()));
    }

    @Test
    void project_defaultsToAFourPercentWithdrawalRate() {
        FireProjectionResultDTO result = this.fireService.project(USER,
                this.plan().withdrawalRate(null).annualSpending(BigDecimal.valueOf(4_000_000)).build());

        assertEquals(0, result.getWithdrawalRate().compareTo(new BigDecimal("4.00")));
        assertEquals(0, result.getFireNumber().compareTo(new BigDecimal("100000000.00")));
    }

    @Test
    void project_defaultsTheEndOfThePlanToNinety() {
        FireProjectionResultDTO result = this.fireService.project(USER,
                this.plan().lifeExpectancy(null).currentAge(30).build());

        assertEquals(90, result.getFinalAge());
    }

    @Test
    void project_passesThroughCurrenciesTheValuationCouldNotConvert() {
        when(this.netWorthService.getNetWorth(anyString(), any(), anyBoolean())).thenReturn(NetWorthDTO.builder()
                .currency("HUF").totalWorth(BigDecimal.ZERO)
                .unconvertedCurrencies(List.of("GBP")).build());

        FireProjectionResultDTO result = this.fireService.project(USER, this.plan().build());

        assertEquals(List.of("GBP"), result.getUnconvertedCurrencies());
    }

    @Test
    void getCSV_writesTheWholeTimelineWithAHeader() {
        StringWriter writer = new StringWriter();

        this.fireService.getCSV(USER, this.plan().currentAge(30).lifeExpectancy(33).build(), writer);

        String[] lines = writer.toString().split("\\r?\\n");
        assertEquals(5, lines.length);
        assertTrue(lines[0].startsWith("Year,Age,Phase"));
        assertNotNull(lines[4]);
    }
}
