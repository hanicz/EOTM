package eye.on.the.money.service.salary;

import eye.on.the.money.dto.in.PensionProjectionDTO;
import eye.on.the.money.dto.in.PensionScenarioInputDTO;
import eye.on.the.money.dto.in.PensionStepDTO;
import eye.on.the.money.dto.out.PensionProjectionResultDTO;
import eye.on.the.money.dto.out.PensionScenarioResultDTO;
import eye.on.the.money.dto.out.PensionSensitivityPointDTO;
import eye.on.the.money.dto.out.PensionStopAgePointDTO;
import eye.on.the.money.dto.out.PensionYearDTO;
import eye.on.the.money.dto.out.SalaryNetDTO;
import eye.on.the.money.exception.ValidationException;
import eye.on.the.money.model.salary.DegressioMode;
import eye.on.the.money.model.salary.PensionScenarioType;
import eye.on.the.money.model.salary.Salary;
import eye.on.the.money.model.salary.SalaryBasis;
import eye.on.the.money.repository.salary.SalaryRepository;
import eye.on.the.money.service.shared.ICSVService;
import eye.on.the.money.service.shared.SalaryTaxCalculator;
import eye.on.the.money.util.HungarianPensionScale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.Writer;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Year;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

@Service
@RequiredArgsConstructor
public class PensionProjectionService implements ICSVService {

    private static final String HUF = "HUF";
    private static final int SCALE = 2;
    private static final int REPLACEMENT_RATE_DEPENDENTS = 0;
    private static final double RATE_EPSILON = 0.05;
    private static final List<Double> SENSITIVITY_RATES = List.of(0.0, 1.0, 2.0, 3.0);

    private final SalaryRepository salaryRepository;
    private final SalaryTaxCalculator salaryTaxCalculator;

    public PensionProjectionResultDTO project(Long userId, PensionProjectionDTO input) {
        List<String> warnings = new ArrayList<>();
        List<Salary> salaries = this.salaryRepository.findByUserIdOrderByValidFromDesc(userId);
        Assumptions assumptions = this.validate(input, salaries, warnings);
        Map<Integer, YearEarning> past = this.pastEarnings(salaries, assumptions, warnings);

        List<PensionScenarioResultDTO> scenarios = input.getScenarios().stream()
                .map(scenario -> this.scenarioResult(scenario, assumptions, past))
                .toList();

        int serviceYears = this.serviceYears(assumptions, assumptions.stopWorkingAge());
        double averagePension = this.nationalAveragePension(assumptions, serviceYears);

        return PensionProjectionResultDTO.builder()
                .currency(HUF)
                .currentAge(assumptions.currentAge())
                .stopWorkingAge(assumptions.stopWorkingAge())
                .retirementAge(assumptions.retirementAge())
                .retirementYear(assumptions.retirementYear())
                .gapYears(assumptions.retirementAge() - assumptions.stopWorkingAge())
                .serviceYears(serviceYears)
                .scalePct(this.scaled(HungarianPensionScale.percentFor(serviceYears)))
                .eligible(serviceYears >= HungarianPensionScale.MIN_SERVICE_YEARS_PARTIAL)
                .partialPension(serviceYears >= HungarianPensionScale.MIN_SERVICE_YEARS_PARTIAL
                        && serviceYears < HungarianPensionScale.MIN_SERVICE_YEARS_FULL)
                .degresszio(assumptions.degresszio())
                .degressioLowerThreshold(
                        this.scaled(this.threshold(HungarianPensionScale.DEGRESSIO_LOWER, assumptions)))
                .degressioUpperThreshold(
                        this.scaled(this.threshold(HungarianPensionScale.DEGRESSIO_UPPER, assumptions)))
                .currentGrossMonthly(this.scaled(assumptions.currentGrossMonthly()))
                .realWageGrowth(this.scaled(assumptions.realWageGrowth() * 100))
                .valorizationUplift(this.scaled(this.valorizationUplift(assumptions)))
                .wageGrowthSensitivity(
                        this.sensitivity(input.getScenarios().getFirst(), assumptions, past))
                .nationalAverageGrossMonthly(
                        this.scaled(HungarianPensionScale.NATIONAL_AVERAGE_GROSS_MONTHLY))
                .nationalAveragePension(this.scaled(averagePension))
                .nationalAverageMultiple(this.scaled((averagePension <= 0) ? 0
                        : scenarios.getFirst().getMonthlyPension().doubleValue() / averagePension))
                .salaryMultipleOfNationalAverage(this.scaled(assumptions.currentGrossMonthly()
                        / HungarianPensionScale.NATIONAL_AVERAGE_GROSS_MONTHLY))
                .scenarios(scenarios)
                .warnings(warnings)
                .build();
    }

    public void getCSV(Long userId, PensionProjectionDTO input, Writer writer) {
        List<PensionYearDTO> rows = this.project(userId, input).getScenarios().stream()
                .flatMap(scenario -> scenario.getTimeline().stream())
                .toList();
        this.printRecords(rows, writer);
    }

    private PensionScenarioResultDTO scenarioResult(PensionScenarioInputDTO scenario, Assumptions assumptions,
                                                    Map<Integer, YearEarning> past) {
        double[] futurePath = this.futurePath(scenario, assumptions);
        Outcome outcome = this.outcome(futurePath, assumptions, past, assumptions.stopWorkingAge());

        List<PensionStopAgePointDTO> byStopAge = new ArrayList<>();
        for (int stopAge = assumptions.currentAge(); stopAge <= assumptions.retirementAge(); stopAge++) {
            Outcome point = this.outcome(futurePath, assumptions, past, stopAge);
            byStopAge.add(PensionStopAgePointDTO.builder()
                    .stopAge(stopAge)
                    .serviceYears(point.serviceYears())
                    .scalePct(this.scaled(point.scalePct()))
                    .monthlyPension(this.scaled(point.monthlyPension()))
                    .build());
        }

        SalaryNetDTO finalNet = this.salaryTaxCalculator.calculate(
                this.scaled(outcome.finalGross()), HUF, REPLACEMENT_RATE_DEPENDENTS);
        double finalNetMonthly = finalNet.getNetMonthly().doubleValue();

        return PensionScenarioResultDTO.builder()
                .name(scenario.name())
                .type(scenario.type())
                .pensionBaseMonthly(this.scaled(outcome.pensionBase()))
                .degressedBaseMonthly(this.scaled(outcome.degressedBase()))
                .monthlyPension(this.scaled(outcome.monthlyPension()))
                .annualPension(this.scaled(outcome.monthlyPension() * assumptions.monthsPaid()))
                .finalGrossMonthly(this.scaled(outcome.finalGross()))
                .finalNetMonthly(this.scaled(finalNetMonthly))
                .replacementRatePct(this.scaled(finalNetMonthly <= 0
                        ? 0 : outcome.monthlyPension() / finalNetMonthly * 100))
                .minimumApplied(outcome.minimumApplied())
                .byStopAge(byStopAge)
                .timeline(this.timeline(scenario, futurePath, assumptions, past))
                .build();
    }

    private List<PensionSensitivityPointDTO> sensitivity(PensionScenarioInputDTO scenario,
                                                         Assumptions assumptions,
                                                         Map<Integer, YearEarning> past) {
        double chosen = this.rounded(assumptions.realWageGrowth() * 100);
        Set<Double> rates = new TreeSet<>(SENSITIVITY_RATES);
        rates.add(chosen);
        double[] futurePath = this.futurePath(scenario, assumptions);

        return rates.stream().map(percent -> {
            Assumptions alternative = assumptions.withRealWageGrowth(percent / 100.0);
            Outcome outcome = this.outcome(futurePath, alternative, past, alternative.stopWorkingAge());
            return PensionSensitivityPointDTO.builder()
                    .realWageGrowth(this.scaled(percent))
                    .valorizationUplift(this.scaled(this.valorizationUplift(alternative)))
                    .monthlyPension(this.scaled(outcome.monthlyPension()))
                    .selected(Math.abs(percent - chosen) < RATE_EPSILON)
                    .build();
        }).toList();
    }

    private double nationalAveragePension(Assumptions assumptions, int serviceYears) {
        double averageBase =
                HungarianPensionScale.pensionNet(HungarianPensionScale.NATIONAL_AVERAGE_GROSS_MONTHLY)
                        * this.valorizationUplift(assumptions);
        return this.applyDegresszio(averageBase, assumptions)
                * HungarianPensionScale.percentFor(serviceYears) / 100;
    }

    private double valorizationUplift(Assumptions assumptions) {
        return Math.pow(1 + assumptions.realWageGrowth(),
                Math.max(0, assumptions.valorizationTargetYear() - assumptions.currentYear()));
    }

    private double rounded(double percent) {
        return Math.round(percent * 10) / 10.0;
    }

    private Outcome outcome(double[] futurePath, Assumptions assumptions, Map<Integer, YearEarning> past,
                            int stopWorkingAge) {
        int serviceYears = this.serviceYears(assumptions, stopWorkingAge);
        int stopYear = assumptions.currentYear() + (stopWorkingAge - assumptions.currentAge());

        double valorizedSum = 0;
        double counted = 0;
        double finalGross = 0;

        for (int year = assumptions.firstCountedYear(); year < stopYear; year++) {
            YearEarning earning = this.earningFor(year, futurePath, assumptions, past);
            if (earning.grossMonthly() <= 0 || earning.dayFraction() <= 0) {
                continue;
            }
            valorizedSum += this.valorize(HungarianPensionScale.pensionNet(earning.grossMonthly()),
                    year, assumptions) * earning.dayFraction();
            counted += earning.dayFraction();
            finalGross = earning.grossMonthly();
        }

        double pensionBase = (counted <= 0) ? 0 : valorizedSum / counted;
        double degressedBase = this.applyDegresszio(pensionBase, assumptions);
        double scalePct = HungarianPensionScale.percentFor(serviceYears);
        double monthlyPension = degressedBase * scalePct / 100;

        boolean minimumApplied = serviceYears >= HungarianPensionScale.MIN_SERVICE_YEARS_FULL
                && monthlyPension > 0
                && monthlyPension < HungarianPensionScale.MINIMUM_PENSION;
        if (minimumApplied) {
            monthlyPension = HungarianPensionScale.MINIMUM_PENSION;
        }

        return new Outcome(serviceYears, scalePct, pensionBase, degressedBase, monthlyPension, finalGross,
                minimumApplied);
    }

    private List<PensionYearDTO> timeline(PensionScenarioInputDTO scenario, double[] futurePath,
                                          Assumptions assumptions, Map<Integer, YearEarning> past) {
        int stopYear = assumptions.currentYear() + (assumptions.stopWorkingAge() - assumptions.currentAge());
        List<PensionYearDTO> rows = new ArrayList<>();

        for (int year = assumptions.firstCountedYear(); year < assumptions.retirementYear(); year++) {
            boolean working = year < stopYear;
            double gross = working
                    ? this.earningFor(year, futurePath, assumptions, past).grossMonthly() : 0;
            double net = HungarianPensionScale.pensionNet(gross);

            rows.add(PensionYearDTO.builder()
                    .scenario(scenario.name())
                    .year(year)
                    .age(assumptions.currentAge() + (year - assumptions.currentYear()))
                    .working(working)
                    .counted(working && gross > 0)
                    .grossMonthly(this.scaled(gross))
                    .netMonthly(this.scaled(net))
                    .valorizedNetMonthly(this.scaled(working ? this.valorize(net, year, assumptions) : 0))
                    .build());
        }
        return rows;
    }

    private YearEarning earningFor(int year, double[] futurePath, Assumptions assumptions,
                                   Map<Integer, YearEarning> past) {
        if (year >= assumptions.currentYear()) {
            int index = year - assumptions.currentYear();
            double gross = (index < futurePath.length)
                    ? futurePath[index] : futurePath[futurePath.length - 1];
            return new YearEarning(gross, 1.0);
        }
        return past.getOrDefault(year, new YearEarning(assumptions.priorAverageGrossMonthly(), 1.0));
    }

    private double valorize(double netMonthly, int year, Assumptions assumptions) {
        int years = Math.max(0, assumptions.valorizationTargetYear() - year);
        return netMonthly * Math.pow(1 + assumptions.realWageGrowth(), years);
    }

    private double applyDegresszio(double base, Assumptions assumptions) {
        if (assumptions.degresszio() == DegressioMode.IGNORED) {
            return base;
        }
        double lower = this.threshold(HungarianPensionScale.DEGRESSIO_LOWER, assumptions);
        double upper = this.threshold(HungarianPensionScale.DEGRESSIO_UPPER, assumptions);

        if (base <= lower) {
            return base;
        }
        if (base <= upper) {
            return lower + (base - lower) * HungarianPensionScale.DEGRESSIO_UPPER_RATE;
        }
        return lower + (upper - lower) * HungarianPensionScale.DEGRESSIO_UPPER_RATE
                + (base - upper) * HungarianPensionScale.DEGRESSIO_TOP_RATE;
    }

    private double threshold(double statutory, Assumptions assumptions) {
        int years = Math.max(0, assumptions.valorizationTargetYear() - assumptions.currentYear());
        return (assumptions.degresszio() == DegressioMode.FROZEN)
                ? statutory / Math.pow(1 + assumptions.inflation(), years)
                : statutory * Math.pow(1 + assumptions.realWageGrowth(), years);
    }

    private double[] futurePath(PensionScenarioInputDTO scenario, Assumptions assumptions) {
        int horizon = assumptions.retirementAge() - assumptions.currentAge();
        double[] path = new double[horizon + 1];
        path[0] = assumptions.currentGrossMonthly();

        double growth = this.rate(scenario.realGrowthPct());
        List<PensionStepDTO> steps = this.sortedSteps(scenario);

        for (int index = 1; index <= horizon; index++) {
            path[index] = switch (scenario.type()) {
                case REAL_FLAT -> path[0];
                case REAL_GROWTH -> path[index - 1] * (1 + growth);
                case NOMINAL_LOCK -> path[index - 1] / (1 + assumptions.inflation());
                case STEPS -> path[index - 1] * (1 + this.stepRate(steps, assumptions.currentAge() + index - 1));
            };
        }
        return path;
    }

    private List<PensionStepDTO> sortedSteps(PensionScenarioInputDTO scenario) {
        return (scenario.steps() == null) ? List.of()
                : scenario.steps().stream().sorted(Comparator.comparingInt(PensionStepDTO::untilAge)).toList();
    }

    private double stepRate(List<PensionStepDTO> steps, int age) {
        return steps.stream()
                .filter(step -> age < step.untilAge())
                .findFirst()
                .map(step -> this.rate(step.realGrowthPct()))
                .orElse(0.0);
    }

    private Map<Integer, YearEarning> pastEarnings(List<Salary> salaries, Assumptions assumptions,
                                                   List<String> warnings) {
        Map<Integer, double[]> byYear = new HashMap<>();
        Set<String> ignoredCurrencies = new LinkedHashSet<>();

        for (Salary salary : salaries) {
            String currencyId = salary.getCurrency().getId();
            if (!HUF.equals(currencyId)) {
                ignoredCurrencies.add(currencyId);
                continue;
            }
            this.accumulate(byYear, salary, assumptions);
        }

        if (!ignoredCurrencies.isEmpty()) {
            warnings.add("Salary records in " + String.join(", ", ignoredCurrencies)
                    + " were left out, because only Hungarian-insured forint earnings build state pension"
                    + " entitlement. Set the average gross before your salary history by hand if those years"
                    + " should count.");
        }

        Map<Integer, YearEarning> earnings = new HashMap<>();
        byYear.forEach((year, cell) -> {
            double days = cell[1];
            double monthly = cell[0] / days;
            double fraction = Math.min(1.0, days / Year.of(year).length());
            earnings.put(year, new YearEarning(this.toTodaysMoney(monthly, year, assumptions), fraction));
        });
        return earnings;
    }

    private void accumulate(Map<Integer, double[]> byYear, Salary salary, Assumptions assumptions) {
        LocalDate from = salary.getValidFrom();
        LocalDate to = (salary.getValidTo() == null) ? LocalDate.now() : salary.getValidTo();
        if (to.isBefore(from)) {
            return;
        }
        double monthly = this.grossMonthly(salary);
        int firstYear = Math.max(from.getYear(), assumptions.firstCountedYear());
        int lastYear = Math.min(to.getYear(), assumptions.currentYear() - 1);

        for (int year = firstYear; year <= lastYear; year++) {
            LocalDate start = this.later(from, LocalDate.of(year, 1, 1));
            LocalDate end = this.earlier(to, LocalDate.of(year, 12, 31));
            if (end.isBefore(start)) {
                continue;
            }
            double days = ChronoUnit.DAYS.between(start, end) + 1.0;
            double[] cell = byYear.computeIfAbsent(year, key -> new double[2]);
            cell[0] += days * monthly;
            cell[1] += days;
        }
    }

    private LocalDate later(LocalDate left, LocalDate right) {
        return left.isAfter(right) ? left : right;
    }

    private LocalDate earlier(LocalDate left, LocalDate right) {
        return left.isBefore(right) ? left : right;
    }

    private double toTodaysMoney(double nominalMonthly, int year, Assumptions assumptions) {
        return nominalMonthly * Math.pow(1 + assumptions.inflation(), assumptions.currentYear() - year);
    }

    private double grossMonthly(Salary salary) {
        return (SalaryBasis.ANNUAL == salary.getBasis())
                ? salary.getAmount().doubleValue() / HungarianPensionScale.MONTHS_IN_YEAR
                : salary.getAmount().doubleValue();
    }

    private int serviceYears(Assumptions assumptions, int stopWorkingAge) {
        return assumptions.yearsAlreadyWorked() + (stopWorkingAge - assumptions.currentAge());
    }

    private Assumptions validate(PensionProjectionDTO input, List<Salary> salaries, List<String> warnings) {
        int currentAge = input.getCurrentAge();
        int stopWorkingAge = input.getStopWorkingAge();
        int retirementAge = (input.getRetirementAge() == null)
                ? HungarianPensionScale.DEFAULT_RETIREMENT_AGE : input.getRetirementAge();

        if (stopWorkingAge < currentAge) {
            throw new ValidationException("The age you stop working must not be before your current age");
        }
        if (retirementAge < stopWorkingAge) {
            throw new ValidationException("The pension cannot start before you stop working");
        }
        this.rejectInvalidSteps(input);

        double inflation = this.rate(input.getInflation(), HungarianPensionScale.DEFAULT_INFLATION);
        double realWageGrowth = this.rate(input.getRealWageGrowth(),
                HungarianPensionScale.DEFAULT_REAL_WAGE_GROWTH);
        int currentYear = LocalDate.now().getYear();
        int retirementYear = currentYear + (retirementAge - currentAge);
        int careerStartYear = currentYear - input.getYearsAlreadyWorked();

        double currentGrossMonthly = this.currentGrossMonthly(input, salaries);
        double priorAverage = this.priorAverage(input, currentGrossMonthly);

        if (careerStartYear < HungarianPensionScale.EARNINGS_START_YEAR) {
            warnings.add("Earnings before " + HungarianPensionScale.EARNINGS_START_YEAR
                    + " do not count towards the pension base, but those years still count as service time.");
        }

        return new Assumptions(currentAge, stopWorkingAge, retirementAge, currentYear, retirementYear,
                Math.max(careerStartYear, HungarianPensionScale.EARNINGS_START_YEAR),
                input.getYearsAlreadyWorked(), currentGrossMonthly, priorAverage, realWageGrowth, inflation,
                (input.getDegresszio() == null) ? DegressioMode.FROZEN : input.getDegresszio(),
                Boolean.FALSE.equals(input.getIncludeThirteenthMonth())
                        ? HungarianPensionScale.MONTHS_IN_YEAR
                        : HungarianPensionScale.THIRTEENTH_MONTH_FACTOR);
    }

    private void rejectInvalidSteps(PensionProjectionDTO input) {
        for (PensionScenarioInputDTO scenario : input.getScenarios()) {
            if (scenario.type() != PensionScenarioType.STEPS) {
                continue;
            }
            if (scenario.steps() == null || scenario.steps().isEmpty()) {
                throw new ValidationException("Add at least one step to the " + scenario.name() + " scenario");
            }
            int previous = Integer.MIN_VALUE;
            for (PensionStepDTO step : this.sortedSteps(scenario)) {
                if (step.untilAge() == previous) {
                    throw new ValidationException("Each step of the " + scenario.name()
                            + " scenario needs its own age");
                }
                previous = step.untilAge();
            }
        }
    }

    private double currentGrossMonthly(PensionProjectionDTO input, List<Salary> salaries) {
        if (input.getCurrentGrossMonthlyOverride() != null
                && input.getCurrentGrossMonthlyOverride().signum() > 0) {
            return input.getCurrentGrossMonthlyOverride().doubleValue();
        }
        return this.currentSalary(salaries)
                .map(this::grossMonthly)
                .orElseThrow(() -> new ValidationException(
                        "Add a forint salary on the History tab, or set your current gross by hand"));
    }

    private Optional<Salary> currentSalary(List<Salary> salaries) {
        List<Salary> inForint = salaries.stream()
                .filter(salary -> HUF.equals(salary.getCurrency().getId()))
                .toList();
        return inForint.stream().filter(salary -> salary.getValidTo() == null).findFirst()
                .or(() -> inForint.stream().findFirst());
    }

    private double priorAverage(PensionProjectionDTO input, double currentGrossMonthly) {
        return (input.getPriorAverageGrossMonthly() == null
                || input.getPriorAverageGrossMonthly().signum() <= 0)
                ? currentGrossMonthly : input.getPriorAverageGrossMonthly().doubleValue();
    }

    private double rate(BigDecimal percent) {
        return this.rate(percent, 0);
    }

    private double rate(BigDecimal percent, double fallback) {
        return ((percent == null) ? fallback : percent.doubleValue()) / 100.0;
    }

    private BigDecimal scaled(double value) {
        if (!Double.isFinite(value)) {
            throw new ValidationException("These assumptions produce numbers too large to project");
        }
        return BigDecimal.valueOf(value).setScale(SCALE, RoundingMode.HALF_UP);
    }

    private record Assumptions(int currentAge, int stopWorkingAge, int retirementAge, int currentYear,
                               int retirementYear, int firstCountedYear, int yearsAlreadyWorked,
                               double currentGrossMonthly, double priorAverageGrossMonthly,
                               double realWageGrowth, double inflation, DegressioMode degresszio,
                               int monthsPaid) {

        private int valorizationTargetYear() {
            return this.retirementYear - 1;
        }

        private Assumptions withRealWageGrowth(double rate) {
            return new Assumptions(this.currentAge, this.stopWorkingAge, this.retirementAge,
                    this.currentYear, this.retirementYear, this.firstCountedYear, this.yearsAlreadyWorked,
                    this.currentGrossMonthly, this.priorAverageGrossMonthly, rate, this.inflation,
                    this.degresszio, this.monthsPaid);
        }
    }

    private record YearEarning(double grossMonthly, double dayFraction) {
    }

    private record Outcome(int serviceYears, double scalePct, double pensionBase, double degressedBase,
                           double monthlyPension, double finalGross, boolean minimumApplied) {
    }
}
