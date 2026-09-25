package eye.on.the.money.service.shared;

import eye.on.the.money.dto.out.FireSnapshotDTO;
import eye.on.the.money.dto.out.MonthlyCashFlowDTO;
import eye.on.the.money.dto.out.NetWorthDTO;
import eye.on.the.money.service.financial.BankTransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FireSnapshotServiceTest {

    private static final Long USER = 1L;
    private static final double TOLERANCE = 0.01;
    private static final YearMonth TODAY = YearMonth.of(2026, 4);

    @Mock
    private NetWorthService netWorthService;

    @Mock
    private BankTransactionService bankTransactionService;

    private FireService fireService;
    private FireSnapshotService snapshotService;

    @BeforeEach
    void setUp() {
        this.fireService = spy(new FireService(this.netWorthService));
        this.snapshotService = new FireSnapshotService(this.fireService, this.netWorthService,
                this.bankTransactionService);
        this.stubPortfolio(0, List.of());
        this.stubCashFlow();
    }

    private void stubPortfolio(double worth, List<String> unconverted) {
        when(this.netWorthService.getNetWorth(anyLong(), any(), anyBoolean())).thenReturn(NetWorthDTO.builder()
                .currency("HUF")
                .totalWorth(BigDecimal.valueOf(worth))
                .unconvertedCurrencies(unconverted)
                .build());
    }

    private void stubCashFlow(MonthlyCashFlowDTO... rows) {
        when(this.bankTransactionService.getMonthlyCashFlow(anyLong())).thenReturn(List.of(rows));
    }

    private MonthlyCashFlowDTO month(int year, int month, String currency, double in, double out) {
        return MonthlyCashFlowDTO.builder()
                .year(year)
                .month(month)
                .currencyId(currency)
                .moneyIn(BigDecimal.valueOf(in))
                .moneyOut(BigDecimal.valueOf(out))
                .build();
    }

    private void stubSteadyHufMonths(double in, double out) {
        this.stubCashFlow(
                this.month(2026, 3, "HUF", in, out),
                this.month(2026, 2, "HUF", in, out),
                this.month(2026, 1, "HUF", in, out));
    }

    @Test
    void snapshot_averagesTheCompleteMonthsItHas() {
        this.stubSteadyHufMonths(2_000_000, -1_200_000);

        FireSnapshotDTO snapshot = this.snapshotService.snapshot(USER, "HUF", TODAY);

        assertTrue(snapshot.isHasCashFlow());
        assertEquals(3, snapshot.getMonthsCounted());
        assertEquals(800_000, snapshot.getMonthlySavings().doubleValue(), TOLERANCE);
        assertEquals(40, snapshot.getSavingsRatePct().doubleValue(), TOLERANCE);
    }

    @Test
    void snapshot_usesTheFixedTargetForTheTargetCurrency() {
        this.stubSteadyHufMonths(2_000_000, -1_200_000);

        FireSnapshotDTO snapshot = this.snapshotService.snapshot(USER, "HUF", TODAY);

        assertEquals(450_000_000, snapshot.getFireNumber().doubleValue(), TOLERANCE);
        assertEquals("FIXED", snapshot.getTargetSource());
        assertEquals(3, snapshot.getWithdrawalRate().doubleValue(), TOLERANCE);
    }

    @Test
    void snapshot_derivesTheTargetFromSpendingInAnotherCurrency() {
        this.stubCashFlow(
                this.month(2026, 3, "EUR", 2_000, -1_000),
                this.month(2026, 2, "EUR", 2_000, -1_000),
                this.month(2026, 1, "EUR", 2_000, -1_000));

        FireSnapshotDTO snapshot = this.snapshotService.snapshot(USER, "EUR", TODAY);

        assertEquals("DERIVED", snapshot.getTargetSource());
        assertEquals(400_000, snapshot.getFireNumber().doubleValue(), TOLERANCE);
    }

    @Test
    void snapshot_hasNoTargetWhenAnotherCurrencyHasNoSpending() {
        this.stubCashFlow(
                this.month(2026, 3, "EUR", 2_000, 0),
                this.month(2026, 2, "EUR", 2_000, 0));

        FireSnapshotDTO snapshot = this.snapshotService.snapshot(USER, "EUR", TODAY);

        assertNull(snapshot.getFireNumber());
        assertNull(snapshot.getProgressPct());
        assertNull(snapshot.getYearsToFire());
        assertEquals(2_000, snapshot.getMonthlySavings().doubleValue(), TOLERANCE);
    }

    @Test
    void snapshot_leavesOutTheCurrentCalendarMonth() {
        this.stubCashFlow(
                this.month(2026, 4, "HUF", 100_000, -90_000),
                this.month(2026, 3, "HUF", 2_000_000, -1_200_000),
                this.month(2026, 2, "HUF", 2_000_000, -1_200_000),
                this.month(2026, 1, "HUF", 2_000_000, -1_200_000));

        FireSnapshotDTO snapshot = this.snapshotService.snapshot(USER, "HUF", TODAY);

        assertEquals(3, snapshot.getMonthsCounted());
        assertEquals(800_000, snapshot.getMonthlySavings().doubleValue(), TOLERANCE);
    }

    @Test
    void snapshot_keepsAStaleNewestMonthThatIsInThePast() {
        this.stubCashFlow(
                this.month(2025, 8, "HUF", 1_000_000, -600_000),
                this.month(2025, 7, "HUF", 1_000_000, -600_000),
                this.month(2025, 6, "HUF", 1_000_000, -600_000));

        FireSnapshotDTO snapshot = this.snapshotService.snapshot(USER, "HUF", TODAY);

        assertTrue(snapshot.isHasCashFlow());
        assertEquals(3, snapshot.getMonthsCounted());
        assertEquals(400_000, snapshot.getMonthlySavings().doubleValue(), TOLERANCE);
    }

    @Test
    void snapshot_averagesAtMostTheNewestTwelveMonths() {
        List<MonthlyCashFlowDTO> rows = new ArrayList<>();
        for (int back = 0; back < 12; back++) {
            YearMonth month = YearMonth.of(2026, 3).minusMonths(back);
            rows.add(this.month(month.getYear(), month.getMonthValue(), "HUF", 2_000_000, -1_200_000));
        }
        rows.add(this.month(2025, 3, "HUF", 10_000_000, -9_000_000));
        this.stubCashFlow(rows.toArray(new MonthlyCashFlowDTO[0]));

        FireSnapshotDTO snapshot = this.snapshotService.snapshot(USER, "HUF", TODAY);

        assertEquals(12, snapshot.getMonthsCounted());
        assertEquals(800_000, snapshot.getMonthlySavings().doubleValue(), TOLERANCE);
        assertEquals(40, snapshot.getSavingsRatePct().doubleValue(), TOLERANCE);
    }

    @Test
    void snapshot_dividesByTheMonthsActuallyPresent() {
        this.stubCashFlow(
                this.month(2026, 3, "HUF", 2_000_000, -1_000_000),
                this.month(2026, 1, "HUF", 1_000_000, -500_000));

        FireSnapshotDTO snapshot = this.snapshotService.snapshot(USER, "HUF", TODAY);

        assertEquals(2, snapshot.getMonthsCounted());
        assertEquals(750_000, snapshot.getMonthlySavings().doubleValue(), TOLERANCE);
        assertEquals(50, snapshot.getSavingsRatePct().doubleValue(), TOLERANCE);
    }

    @Test
    void snapshot_countsOnlyRowsInThePreferredCurrency() {
        this.stubCashFlow(
                this.month(2026, 3, "HUF", 2_000_000, -1_200_000),
                this.month(2026, 3, "EUR", 5_000, -4_000),
                this.month(2026, 2, "HUF", 2_000_000, -1_200_000),
                this.month(2026, 1, "HUF", 2_000_000, -1_200_000));

        FireSnapshotDTO snapshot = this.snapshotService.snapshot(USER, "HUF", TODAY);

        assertEquals(3, snapshot.getMonthsCounted());
        assertEquals(800_000, snapshot.getMonthlySavings().doubleValue(), TOLERANCE);
        assertEquals(List.of("EUR"), snapshot.getIgnoredCurrencies());
    }

    @Test
    void snapshot_passesThePortfolioUnconvertedCurrenciesThrough() {
        this.stubPortfolio(0, List.of("GBP"));
        this.stubSteadyHufMonths(2_000_000, -1_200_000);

        FireSnapshotDTO snapshot = this.snapshotService.snapshot(USER, "HUF", TODAY);

        assertEquals(List.of("GBP"), snapshot.getUnconvertedCurrencies());
    }

    @Test
    void snapshot_stillReportsTheTargetWithoutAnyCashFlow() {
        this.stubPortfolio(90_000_000, List.of());

        FireSnapshotDTO snapshot = this.snapshotService.snapshot(USER, "HUF", TODAY);

        assertFalse(snapshot.isHasCashFlow());
        assertEquals(0, snapshot.getMonthsCounted());
        assertEquals(450_000_000, snapshot.getFireNumber().doubleValue(), TOLERANCE);
        assertEquals(20, snapshot.getProgressPct().doubleValue(), TOLERANCE);
        assertNull(snapshot.getYearsToFire());
        verify(this.fireService, never()).project(anyLong(), any());
    }

    @Test
    void snapshot_treatsAPartialCurrentMonthAloneAsNoCashFlow() {
        this.stubCashFlow(this.month(2026, 4, "HUF", 2_000_000, -1_200_000));

        FireSnapshotDTO snapshot = this.snapshotService.snapshot(USER, "HUF", TODAY);

        assertFalse(snapshot.isHasCashFlow());
        assertNull(snapshot.getYearsToFire());
        verify(this.fireService, never()).project(anyLong(), any());
    }

    @Test
    void snapshot_neverReachesTheTargetWhenSpendingExceedsIncome() {
        this.stubSteadyHufMonths(1_000_000, -1_500_000);

        FireSnapshotDTO snapshot = this.snapshotService.snapshot(USER, "HUF", TODAY);

        assertEquals(-500_000, snapshot.getMonthlySavings().doubleValue(), TOLERANCE);
        assertNull(snapshot.getYearsToFire());
        assertFalse(snapshot.isFiReached());
    }

    @Test
    void snapshot_reportsNoYearsLeftWhenNetWorthIsAlreadyAboveTheTarget() {
        this.stubPortfolio(500_000_000, List.of());
        this.stubSteadyHufMonths(2_000_000, -1_200_000);

        FireSnapshotDTO snapshot = this.snapshotService.snapshot(USER, "HUF", TODAY);

        assertTrue(snapshot.isFiReached());
        assertEquals(0, snapshot.getYearsToFire());
    }

    @Test
    void snapshot_leavesTheSavingsRateBlankWhenNothingCameIn() {
        this.stubSteadyHufMonths(0, -500_000);

        FireSnapshotDTO snapshot = this.snapshotService.snapshot(USER, "HUF", TODAY);

        assertNull(snapshot.getSavingsRatePct());
        assertEquals(-500_000, snapshot.getMonthlySavings().doubleValue(), TOLERANCE);
    }

    @Test
    void snapshot_reportsProgressAsTheNetWorthShareOfTheTarget() {
        this.stubPortfolio(90_000_000, List.of());
        this.stubSteadyHufMonths(2_000_000, -1_200_000);

        FireSnapshotDTO snapshot = this.snapshotService.snapshot(USER, "HUF", TODAY);

        assertEquals(90_000_000, snapshot.getNetWorth().doubleValue(), TOLERANCE);
        assertEquals(20, snapshot.getProgressPct().doubleValue(), TOLERANCE);
        assertFalse(snapshot.isFiReached());
    }

    @Test
    void snapshot_projectsAYearCountFromTheDerivedContribution() {
        this.stubSteadyHufMonths(2_000_000, -1_200_000);

        FireSnapshotDTO snapshot = this.snapshotService.snapshot(USER, "HUF", TODAY);

        assertEquals(22, snapshot.getYearsToFire());
    }

    @Test
    void snapshot_reportsTheAssumptionsItWasRunOn() {
        this.stubSteadyHufMonths(2_000_000, -1_200_000);

        FireSnapshotDTO snapshot = this.snapshotService.snapshot(USER, "HUF", TODAY);

        assertEquals(5.5, snapshot.getAnnualReturn().doubleValue(), TOLERANCE);
        assertEquals(2.5, snapshot.getAnnualContributionIncrease().doubleValue(), TOLERANCE);
    }
}
