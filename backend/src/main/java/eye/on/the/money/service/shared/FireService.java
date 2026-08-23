package eye.on.the.money.service.shared;

import eye.on.the.money.dto.in.FireProjectionDTO;
import eye.on.the.money.dto.out.FireProjectionResultDTO;
import eye.on.the.money.dto.out.FireYearDTO;
import eye.on.the.money.dto.out.NetWorthDTO;
import eye.on.the.money.exception.FireException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.Writer;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Projects a portfolio forward to work out when its owner could stop working, and whether the money lasts
 * once they have.
 * <p>
 * The plan runs as one continuous timeline from today to the end of life. Up to the retirement year the pot
 * grows and takes contributions; after it, contributions stop and an inflation-linked income is drawn from
 * it. Growth is compounded monthly and contributions land at the end of each month.
 * <p>
 * Two figures are reported for every year. The nominal balance is the number that would be on the statement;
 * the real balance discounts that back to today's money. Which one the target is measured against depends on
 * how the target was set, because the two ways of setting it are quoted in different money:
 * <ul>
 *   <li>A FIRE number typed in directly is a plain amount of money. Asking for 300M means wanting to see
 *       300M in the account, so it is measured against the nominal balance.</li>
 *   <li>A FIRE number worked out from annual spending inherits that spending's units, and spending is given
 *       in today's money. It is therefore measured against the real balance; using the nominal one would
 *       call financial independence years too early.</li>
 * </ul>
 * The same choice drives how far along each year is reported to be.
 * <p>
 * A pension, if there is one, is income from its own age onwards regardless of when the pot is retired on.
 * In retirement it meets the spending first and only the shortfall is drawn from the pot; if it more than
 * covers the spending, or arrives while contributions are still being paid, the surplus joins the pot. Like
 * spending, it is given in today's money and inflated to the year it is paid, so it is assumed index-linked.
 * <p>
 * A single fixed return rate is a deliberate simplification. Real markets do not deliver the average every
 * year, and a run of poor returns early in retirement damages a pot far more than the same returns later.
 * Treat the drawdown as an illustration of the assumptions, not a probability.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FireService implements ICSVService {

    private static final List<Integer> MILESTONE_YEARS = List.of(1, 3, 5, 10, 15, 20);

    private static final String ACCUMULATION = "ACCUMULATION";
    private static final String DRAWDOWN = "DRAWDOWN";

    private static final int MONTHS_IN_YEAR = 12;
    private static final int DEFAULT_LIFE_EXPECTANCY = 90;
    private static final double DEFAULT_WITHDRAWAL_RATE = 4.0;
    private static final int SCALE = 2;

    private final NetWorthService netWorthService;

    public FireProjectionResultDTO project(String userEmail, FireProjectionDTO input) {
        log.trace("Enter");
        Assumptions assumptions = this.validate(input);

        NetWorthDTO portfolio = this.netWorthService.getNetWorth(userEmail, input.getCurrency(), false);
        double startingValue = portfolio.getTotalWorth().doubleValue() + assumptions.otherAssets();

        // The retirement year is only known once we know when the target is cleared, so accumulation is run
        // first on its own, then the whole timeline is rebuilt with the drawdown in place.
        List<FireYearDTO> accumulationOnly = this.simulate(startingValue, assumptions, Integer.MAX_VALUE, 0);
        Integer fiYear = this.firstYearAtTarget(accumulationOnly, assumptions);

        Integer retirementYear = this.retirementYear(assumptions, fiYear);
        double annualSpending = this.annualSpendingInTodaysMoney(assumptions, retirementYear);

        List<FireYearDTO> timeline = (retirementYear == null)
                ? accumulationOnly
                : this.simulate(startingValue, assumptions, retirementYear, annualSpending);

        FireYearDTO last = timeline.getLast();
        Integer depletedAtAge = this.depletedAtAge(timeline);

        return FireProjectionResultDTO.builder()
                .currency(portfolio.getCurrency())
                .portfolioValue(portfolio.getTotalWorth())
                .otherAssets(this.scaled(assumptions.otherAssets()))
                .startingValue(this.scaled(startingValue))
                .unconvertedCurrencies(portfolio.getUnconvertedCurrencies())
                .timeline(timeline)
                .milestones(this.milestones(timeline, fiYear))
                .fireNumber(this.scaled(assumptions.fireNumber()))
                .fireNumberInTodaysMoney(assumptions.targetInTodaysMoney())
                .annualSpending(this.scaled(annualSpending))
                .firstYearWithdrawal(this.scaled(this.firstYearWithdrawal(assumptions, retirementYear, annualSpending)))
                .withdrawalRate(this.scaled(assumptions.withdrawalRate()))
                .fireNumberOverridden(assumptions.fireNumberOverridden())
                .fiReached(fiYear != null)
                .fiYear(fiYear)
                .fiAge((fiYear == null) ? null : assumptions.currentAge() + fiYear)
                .retirementYear(retirementYear)
                .retirementAge((retirementYear == null) ? null : assumptions.currentAge() + retirementYear)
                .depletedAtAge(depletedAtAge)
                .lastsThroughRetirement(depletedAtAge == null)
                .finalAge(last.getAge())
                .finalBalance(last.getBalance())
                .finalRealBalance(last.getRealBalance())
                .build();
    }

    public void getCSV(String userEmail, FireProjectionDTO input, Writer writer) {
        log.trace("Enter");
        this.printRecords(this.project(userEmail, input).getTimeline(), writer);
    }

    /**
     * Walks the plan year by year. Contributions run until {@code retirementYear}, withdrawals from the year
     * after; passing {@link Integer#MAX_VALUE} keeps it accumulating for the whole horizon.
     */
    private List<FireYearDTO> simulate(double startingValue, Assumptions assumptions, int retirementYear,
                                       double annualSpending) {
        double monthlyGrowth = Math.pow(1 + assumptions.annualReturn() / 100.0, 1.0 / MONTHS_IN_YEAR);
        double inflationRate = 1 + assumptions.inflation() / 100.0;
        double contributionRise = 1 + assumptions.contributionIncrease() / 100.0;

        List<FireYearDTO> timeline = new ArrayList<>();
        timeline.add(this.year(0, assumptions, ACCUMULATION, 0, 0, 0, 0, startingValue));

        double balance = startingValue;
        boolean depleted = false;

        for (int year = 1; year <= assumptions.horizon(); year++) {
            boolean accumulating = year <= retirementYear;
            double inflationToYear = Math.pow(inflationRate, year);

            // Contributions rise once a year; spending and pension are given in today's money and inflated
            // to the year they land in. Everything is then applied in twelve equal monthly steps.
            double monthlyContribution = accumulating
                    ? assumptions.monthlyContribution() * Math.pow(contributionRise, year - 1) : 0;
            double monthlySpending = accumulating
                    ? 0 : annualSpending * inflationToYear / MONTHS_IN_YEAR;
            double monthlyPension = this.pensionPaid(assumptions, year)
                    ? assumptions.monthlyPension() * inflationToYear : 0;

            // The pension meets the spending first, so only the shortfall comes out of the pot. Anything
            // left over is income with nowhere else to go, so it joins the pot.
            double monthlyIn = monthlyContribution + Math.max(0, monthlyPension - monthlySpending);
            double monthlyOut = Math.max(0, monthlySpending - monthlyPension);

            double contributed = 0;
            double pension = 0;
            double withdrawn = 0;
            double earned = 0;
            for (int month = 0; month < MONTHS_IN_YEAR && !depleted; month++) {
                double growth = balance * (monthlyGrowth - 1);
                balance = balance + growth + monthlyIn - monthlyOut;
                earned += growth;
                contributed += monthlyContribution;
                pension += monthlyPension;
                withdrawn += monthlyOut;
                if (balance <= 0) {
                    // The last withdrawal only partly landed; report what was actually taken.
                    withdrawn += balance;
                    balance = 0;
                    depleted = true;
                }
            }

            timeline.add(this.year(year, assumptions, accumulating ? ACCUMULATION : DRAWDOWN,
                    contributed, earned, pension, withdrawn, balance));
        }
        return timeline;
    }

    private FireYearDTO year(int year, Assumptions assumptions, String phase, double contributed,
                             double earned, double pension, double withdrawn, double balance) {
        double real = balance / Math.pow(1 + assumptions.inflation() / 100.0, year);
        double measured = assumptions.targetInTodaysMoney() ? real : balance;
        return FireYearDTO.builder()
                .year(year)
                .age(assumptions.currentAge() + year)
                .phase(phase)
                .contributions(this.scaled(contributed))
                .growth(this.scaled(earned))
                .pension(this.scaled(pension))
                .withdrawals(this.scaled(withdrawn))
                .balance(this.scaled(balance))
                .realBalance(this.scaled(real))
                .pctOfFireNumber(this.scaled(measured / assumptions.fireNumber() * 100))
                .build();
    }

    /** Financial independence is the first year the pot covers the target, in whichever money it was set. */
    private Integer firstYearAtTarget(List<FireYearDTO> timeline, Assumptions assumptions) {
        return timeline.stream()
                .filter(point -> point.getYear() > 0)
                .filter(point -> this.measuredBalance(point, assumptions) >= assumptions.fireNumber())
                .map(FireYearDTO::getYear)
                .findFirst()
                .orElse(null);
    }

    private double measuredBalance(FireYearDTO point, Assumptions assumptions) {
        return assumptions.targetInTodaysMoney()
                ? point.getRealBalance().doubleValue() : point.getBalance().doubleValue();
    }

    /**
     * The drawdown works in today's money and inflates each year's withdrawal to the year it is spent in, so
     * a target set as a plain future amount has to be brought back to today first. A typed 300M at 4% buys
     * 12M of that year's money; what that is worth today depends on how long it takes to get there.
     */
    private double annualSpendingInTodaysMoney(Assumptions assumptions, Integer retirementYear) {
        if (assumptions.targetInTodaysMoney()) return assumptions.annualSpending();

        double atRetirement = assumptions.fireNumber() * assumptions.withdrawalRate() / 100;
        int years = (retirementYear == null) ? 0 : retirementYear;
        return atRetirement / Math.pow(1 + assumptions.inflation() / 100.0, years);
    }

    /** The cash actually drawn in the first year of retirement, in the money of that year. */
    private double firstYearWithdrawal(Assumptions assumptions, Integer retirementYear, double annualSpending) {
        if (retirementYear == null) return 0;
        return annualSpending * Math.pow(1 + assumptions.inflation() / 100.0, retirementYear + 1);
    }

    /**
     * A stated retirement age wins; otherwise retirement happens as soon as the target is cleared. Null means
     * no drawdown to model, because the plan never gets there.
     */
    private Integer retirementYear(Assumptions assumptions, Integer fiYear) {
        if (assumptions.retirementAge() == null) return fiYear;

        int year = assumptions.retirementAge() - assumptions.currentAge();
        if (year < 0) {
            throw new FireException("Retirement age must not be before your current age");
        }
        if (year > assumptions.horizon()) {
            throw new FireException("Retirement age must be below the age you are planning to");
        }
        return year;
    }

    /** The pension starts at its own age, whether or not the pot has been retired on by then. */
    private boolean pensionPaid(Assumptions assumptions, int year) {
        return assumptions.pensionStartYear() != null
                && assumptions.monthlyPension() > 0
                && year >= assumptions.pensionStartYear();
    }

    private Integer depletedAtAge(List<FireYearDTO> timeline) {
        return timeline.stream()
                .filter(point -> DRAWDOWN.equals(point.getPhase()))
                .filter(point -> point.getBalance().signum() <= 0)
                .map(FireYearDTO::getAge)
                .findFirst()
                .orElse(null);
    }

    /**
     * The fixed horizons, plus the year the target is cleared. That year is the point of the whole exercise
     * and rarely lands on one of the round numbers, so it is worth a row of its own.
     */
    private List<FireYearDTO> milestones(List<FireYearDTO> timeline, Integer fiYear) {
        Set<Integer> years = new TreeSet<>(MILESTONE_YEARS);
        if (fiYear != null) years.add(fiYear);
        return timeline.stream().filter(point -> years.contains(point.getYear())).toList();
    }

    private Assumptions validate(FireProjectionDTO input) {
        int currentAge = input.getCurrentAge();
        int lifeExpectancy = (input.getLifeExpectancy() == null)
                ? DEFAULT_LIFE_EXPECTANCY : input.getLifeExpectancy();
        if (lifeExpectancy <= currentAge) {
            throw new FireException("The age you are planning to must be above your current age");
        }
        // At -100% there is nothing left to discount by, and today's money stops being defined.
        if (input.getInflation().doubleValue() <= -100) {
            throw new FireException("Inflation must be above -100%");
        }

        double monthlyPension = this.value(input.getMonthlyPension(), 0);
        Integer pensionStartYear = null;
        if (input.getPensionAge() != null && monthlyPension > 0) {
            // A pension already in payment simply runs from the start of the plan.
            pensionStartYear = Math.max(0, input.getPensionAge() - currentAge);
        }

        double withdrawalRate = this.value(input.getWithdrawalRate(), DEFAULT_WITHDRAWAL_RATE);
        double annualSpending;
        double fireNumber;
        boolean overridden = this.isPositive(input.getFireNumber());

        if (overridden) {
            // A typed target is a plain amount of money, so it is not in today's money and the spending it
            // supports cannot be settled until the year it is reached is known.
            fireNumber = input.getFireNumber().doubleValue();
            annualSpending = 0;
        } else if (this.isPositive(input.getAnnualSpending())) {
            annualSpending = input.getAnnualSpending().doubleValue();
            fireNumber = annualSpending / (withdrawalRate / 100);
        } else {
            throw new FireException("Set either the annual spending you are aiming for or a FIRE number");
        }

        return new Assumptions(
                this.value(input.getOtherAssets(), 0),
                input.getMonthlyContribution().doubleValue(),
                this.value(input.getAnnualContributionIncrease(), 0),
                input.getAnnualReturn().doubleValue(),
                input.getInflation().doubleValue(),
                annualSpending,
                withdrawalRate,
                fireNumber,
                overridden,
                !overridden,
                monthlyPension,
                pensionStartYear,
                currentAge,
                input.getRetirementAge(),
                lifeExpectancy - currentAge);
    }

    private boolean isPositive(BigDecimal value) {
        return value != null && value.signum() > 0;
    }

    private double value(BigDecimal value, double fallback) {
        return (value == null) ? fallback : value.doubleValue();
    }

    private BigDecimal scaled(double value) {
        // Extreme assumptions can send a figure past what a double holds; BigDecimal cannot take those, and
        // a clear message beats the NumberFormatException it would otherwise throw.
        if (!Double.isFinite(value)) {
            throw new FireException("These assumptions produce numbers too large to project");
        }
        return BigDecimal.valueOf(value).setScale(SCALE, RoundingMode.HALF_UP);
    }

    /** The request once defaults are filled in and the target has been resolved, in plain numbers. */
    private record Assumptions(double otherAssets, double monthlyContribution, double contributionIncrease,
                               double annualReturn, double inflation, double annualSpending,
                               double withdrawalRate, double fireNumber, boolean fireNumberOverridden,
                               boolean targetInTodaysMoney, double monthlyPension, Integer pensionStartYear,
                               int currentAge, Integer retirementAge, int horizon) {
    }
}
