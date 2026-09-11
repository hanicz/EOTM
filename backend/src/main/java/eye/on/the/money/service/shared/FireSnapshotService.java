package eye.on.the.money.service.shared;

import eye.on.the.money.dto.in.FireProjectionDTO;
import eye.on.the.money.dto.out.FireProjectionResultDTO;
import eye.on.the.money.dto.out.FireSnapshotDTO;
import eye.on.the.money.dto.out.MonthlyCashFlowDTO;
import eye.on.the.money.dto.out.NetWorthDTO;
import eye.on.the.money.service.financial.BankTransactionService;
import eye.on.the.money.util.FireDefaults;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FireSnapshotService {

    private static final String BASE_CURRENCY = "EUR";
    private static final String FIXED = "FIXED";
    private static final String DERIVED = "DERIVED";
    private static final int MONTHS_IN_YEAR = 12;
    private static final int SCALE = 2;

    private final FireService fireService;
    private final NetWorthService netWorthService;
    private final BankTransactionService bankTransactionService;

    public FireSnapshotDTO snapshot(Long userId, String currency) {
        return this.snapshot(userId, currency, YearMonth.now());
    }

    FireSnapshotDTO snapshot(Long userId, String currency, YearMonth today) {
        String target = (currency == null || currency.isBlank()) ? BASE_CURRENCY : currency.toUpperCase();

        List<MonthlyCashFlowDTO> complete = this.bankTransactionService.getMonthlyCashFlow(userId).stream()
                .filter(row -> this.monthIndex(row) < this.monthIndexOf(today))
                .toList();

        List<MonthlyCashFlowDTO> window = this.window(complete, target);
        Averages averages = this.averages(window);

        FireSnapshotDTO.FireSnapshotDTOBuilder snapshot = FireSnapshotDTO.builder()
                .currency(target)
                .monthlyIncome(this.scaled(averages.income()))
                .monthlySpending(this.scaled(averages.spending()))
                .monthlySavings(this.scaled(averages.savings()))
                .savingsRatePct(averages.income() == 0 ? null
                        : this.scaled(averages.savings() / averages.income() * 100))
                .withdrawalRate(this.scaled(FireDefaults.SNAPSHOT_WITHDRAWAL_RATE))
                .annualReturn(this.scaled(FireDefaults.ANNUAL_RETURN))
                .annualContributionIncrease(this.scaled(FireDefaults.SNAPSHOT_CONTRIBUTION_INCREASE))
                .inflation(this.scaled(FireDefaults.INFLATION))
                .horizonYears(FireDefaults.LIFE_EXPECTANCY - FireDefaults.SNAPSHOT_AGE)
                .hasCashFlow(!window.isEmpty())
                .monthsCounted(averages.months())
                .windowStart(this.label(window, Comparator.comparingInt(this::monthIndex)))
                .windowEnd(this.label(window, Comparator.comparingInt(this::monthIndex).reversed()))
                .ignoredCurrencies(this.ignored(
                        window.isEmpty() ? complete : this.inSameMonths(complete, window), target));

        Double fireNumber = this.target(target, averages.spending());
        if (fireNumber == null) {
            return this.withoutTarget(userId, target, snapshot);
        }
        if (window.isEmpty()) {
            return this.withoutProjection(userId, target, fireNumber, snapshot);
        }
        return this.withProjection(userId, target, fireNumber, averages, snapshot);
    }

    private FireSnapshotDTO withProjection(Long userId, String target, double fireNumber, Averages averages,
                                           FireSnapshotDTO.FireSnapshotDTOBuilder snapshot) {
        FireProjectionResultDTO projection = this.fireService.project(userId, FireProjectionDTO.builder()
                .currency(target)
                .otherAssets(BigDecimal.ZERO)
                .monthlyContribution(BigDecimal.valueOf(averages.savings()))
                .annualContributionIncrease(BigDecimal.valueOf(FireDefaults.SNAPSHOT_CONTRIBUTION_INCREASE))
                .annualReturn(BigDecimal.valueOf(FireDefaults.ANNUAL_RETURN))
                .inflation(BigDecimal.valueOf(FireDefaults.INFLATION))
                .fireNumber(BigDecimal.valueOf(fireNumber))
                .withdrawalRate(BigDecimal.valueOf(FireDefaults.SNAPSHOT_WITHDRAWAL_RATE))
                .currentAge(FireDefaults.SNAPSHOT_AGE)
                .lifeExpectancy(FireDefaults.LIFE_EXPECTANCY)
                .build());

        double netWorth = projection.getPortfolioValue().doubleValue();
        boolean reached = netWorth >= fireNumber;

        return snapshot
                .netWorth(projection.getPortfolioValue())
                .fireNumber(projection.getFireNumber())
                .targetSource(this.source(target))
                .progressPct(this.scaled(netWorth / fireNumber * 100))
                .yearsToFire(reached ? Integer.valueOf(0) : projection.getFiYear())
                .fiReached(reached)
                .unconvertedCurrencies(projection.getUnconvertedCurrencies())
                .build();
    }

    private FireSnapshotDTO withoutProjection(Long userId, String target, double fireNumber,
                                              FireSnapshotDTO.FireSnapshotDTOBuilder snapshot) {
        NetWorthDTO portfolio = this.netWorthService.getNetWorth(userId, target, false);
        double netWorth = portfolio.getTotalWorth().doubleValue();
        boolean reached = netWorth >= fireNumber;

        return snapshot
                .netWorth(portfolio.getTotalWorth())
                .fireNumber(this.scaled(fireNumber))
                .targetSource(this.source(target))
                .progressPct(this.scaled(netWorth / fireNumber * 100))
                .yearsToFire(reached ? Integer.valueOf(0) : null)
                .fiReached(reached)
                .unconvertedCurrencies(portfolio.getUnconvertedCurrencies())
                .build();
    }

    private FireSnapshotDTO withoutTarget(Long userId, String target,
                                          FireSnapshotDTO.FireSnapshotDTOBuilder snapshot) {
        NetWorthDTO portfolio = this.netWorthService.getNetWorth(userId, target, false);

        return snapshot
                .netWorth(portfolio.getTotalWorth())
                .unconvertedCurrencies(portfolio.getUnconvertedCurrencies())
                .build();
    }

    private Double target(String currency, double monthlySpending) {
        if (FireDefaults.TARGET_CURRENCY.equals(currency)) {
            return FireDefaults.TARGET_AMOUNT;
        }
        if (monthlySpending <= 0) {
            return null;
        }
        return monthlySpending * MONTHS_IN_YEAR / (FireDefaults.SNAPSHOT_WITHDRAWAL_RATE / 100);
    }

    private String source(String currency) {
        return FireDefaults.TARGET_CURRENCY.equals(currency) ? FIXED : DERIVED;
    }

    private List<MonthlyCashFlowDTO> window(List<MonthlyCashFlowDTO> complete, String currency) {
        List<MonthlyCashFlowDTO> mine = complete.stream()
                .filter(row -> currency.equals(row.getCurrencyId()))
                .toList();
        if (mine.isEmpty()) {
            return List.of();
        }

        int anchor = mine.stream().mapToInt(this::monthIndex).max().orElseThrow();
        return mine.stream()
                .filter(row -> this.monthIndex(row) > anchor - FireDefaults.WINDOW_MONTHS)
                .filter(row -> this.monthIndex(row) <= anchor)
                .toList();
    }

    private Averages averages(List<MonthlyCashFlowDTO> window) {
        int months = (int) window.stream().mapToInt(this::monthIndex).distinct().count();
        if (months == 0) {
            return new Averages(0, 0, 0, 0);
        }

        double income = window.stream().mapToDouble(row -> this.value(row.getMoneyIn())).sum() / months;
        double spending = -window.stream().mapToDouble(row -> this.value(row.getMoneyOut())).sum() / months;
        return new Averages(income, spending, income - spending, months);
    }

    private List<MonthlyCashFlowDTO> inSameMonths(List<MonthlyCashFlowDTO> complete,
                                                  List<MonthlyCashFlowDTO> window) {
        Set<Integer> months = window.stream().map(this::monthIndex).collect(Collectors.toSet());
        return complete.stream().filter(row -> months.contains(this.monthIndex(row))).toList();
    }

    private List<String> ignored(List<MonthlyCashFlowDTO> rows, String currency) {
        return rows.stream()
                .map(MonthlyCashFlowDTO::getCurrencyId)
                .filter(found -> found != null && !currency.equals(found))
                .collect(Collectors.toCollection(TreeSet::new))
                .stream()
                .toList();
    }

    private String label(List<MonthlyCashFlowDTO> window, Comparator<MonthlyCashFlowDTO> order) {
        return window.stream()
                .min(order)
                .map(row -> String.format("%04d-%02d", row.getYear(), row.getMonth()))
                .orElse(null);
    }

    private int monthIndex(MonthlyCashFlowDTO row) {
        return row.getYear() * MONTHS_IN_YEAR + (row.getMonth() - 1);
    }

    private int monthIndexOf(YearMonth month) {
        return month.getYear() * MONTHS_IN_YEAR + (month.getMonthValue() - 1);
    }

    private double value(Double amount) {
        return (amount == null) ? 0 : amount;
    }

    private BigDecimal scaled(double value) {
        return BigDecimal.valueOf(value).setScale(SCALE, RoundingMode.HALF_UP);
    }

    private record Averages(double income, double spending, double savings, int months) {
    }
}
