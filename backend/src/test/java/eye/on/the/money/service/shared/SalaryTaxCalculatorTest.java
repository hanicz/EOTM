package eye.on.the.money.service.shared;

import eye.on.the.money.dto.out.SalaryNetDTO;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ActiveProfiles("test")
class SalaryTaxCalculatorTest {

    private static final String HUF = "HUF";
    private static final String EUR = "EUR";

    private final SalaryTaxCalculator salaryTaxCalculator = new SalaryTaxCalculator();

    private void assertAmount(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual),
                () -> "expected " + expected + " but was " + actual);
    }

    private SalaryNetDTO forint(String gross, int dependents) {
        return this.salaryTaxCalculator.calculate(new BigDecimal(gross), HUF, dependents);
    }

    @Test
    void calculate_withoutDependents_takesTheFullSzjaAndContribution() {
        SalaryNetDTO net = this.forint("600000", 0);

        this.assertAmount("90000", net.getSzjaMonthly());
        this.assertAmount("111000", net.getTbMonthly());
        this.assertAmount("399000", net.getNetMonthly());
        assertFalse(net.isFamilyAllowanceApplied());
    }

    @Test
    void calculate_withOneDependent_reducesTheSzjaBase() {
        SalaryNetDTO net = this.forint("600000", 1);

        this.assertAmount("133340", net.getFamilyAllowanceMonthly());
        this.assertAmount("69999", net.getSzjaMonthly());
        this.assertAmount("111000", net.getTbMonthly());
        this.assertAmount("419001", net.getNetMonthly());
        assertTrue(net.isFamilyAllowanceApplied());
    }

    @Test
    void calculate_withTwoDependents_countsTheAllowancePerChild() {
        SalaryNetDTO net = this.forint("600000", 2);

        this.assertAmount("533320", net.getFamilyAllowanceMonthly());
        this.assertAmount("10002", net.getSzjaMonthly());
        this.assertAmount("111000", net.getTbMonthly());
        this.assertAmount("478998", net.getNetMonthly());
    }

    @Test
    void calculate_whenTheAllowanceExceedsTheBase_spillsOverIntoTheContribution() {
        SalaryNetDTO net = this.forint("600000", 3);

        this.assertAmount("1320000", net.getFamilyAllowanceMonthly());
        this.assertAmount("0", net.getSzjaMonthly());
        this.assertAmount("3000", net.getTbMonthly());
        this.assertAmount("597000", net.getNetMonthly());
    }

    @Test
    void calculate_whenTheSpilloverCoversEverything_leavesTheGrossUntouched() {
        SalaryNetDTO net = this.forint("100000", 3);

        this.assertAmount("0", net.getSzjaMonthly());
        this.assertAmount("0", net.getTbMonthly());
        this.assertAmount("100000", net.getNetMonthly());
    }

    @Test
    void calculate_onAHigherSalary_leavesABaseTheAllowanceCannotSwallow() {
        SalaryNetDTO twoDependents = this.forint("1200000", 2);
        SalaryNetDTO threeDependents = this.forint("1200000", 3);

        this.assertAmount("877998", twoDependents.getNetMonthly());
        this.assertAmount("204000", threeDependents.getTbMonthly());
        this.assertAmount("996000", threeDependents.getNetMonthly());
    }

    @Test
    void calculate_reproducesThePublishedMonthlyGainPerDependentCount() {
        BigDecimal withoutDependents = this.forint("2000000", 0).getNetMonthly();
        this.assertAmount("1330000", withoutDependents);

        this.assertAmount("20001", this.forint("2000000", 1).getNetMonthly().subtract(withoutDependents));
        this.assertAmount("79998", this.forint("2000000", 2).getNetMonthly().subtract(withoutDependents));
        this.assertAmount("198000", this.forint("2000000", 3).getNetMonthly().subtract(withoutDependents));
        this.assertAmount("264000", this.forint("2000000", 4).getNetMonthly().subtract(withoutDependents));
    }

    @Test
    void calculate_inAnotherCurrency_leavesOutTheForintAllowance() {
        SalaryNetDTO net = this.salaryTaxCalculator.calculate(new BigDecimal("3000"), EUR, 2);

        this.assertAmount("450", net.getSzjaMonthly());
        this.assertAmount("555", net.getTbMonthly());
        this.assertAmount("1995", net.getNetMonthly());
        this.assertAmount("0", net.getFamilyAllowanceMonthly());
        assertFalse(net.isFamilyAllowanceApplied());
    }

    @Test
    void calculate_withoutAGross_returnsZeros() {
        SalaryNetDTO missing = this.salaryTaxCalculator.calculate(null, HUF, 2);
        SalaryNetDTO negative = this.forint("-1", 2);

        this.assertAmount("0", missing.getNetMonthly());
        this.assertAmount("0", negative.getNetMonthly());
        this.assertAmount("0", negative.getSzjaMonthly());
        this.assertAmount("0", negative.getTbMonthly());
    }
}
