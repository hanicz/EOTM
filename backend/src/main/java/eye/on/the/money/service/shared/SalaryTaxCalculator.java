package eye.on.the.money.service.shared;

import eye.on.the.money.dto.out.SalaryNetDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
@Slf4j
public class SalaryTaxCalculator {

    private static final String HUF = "HUF";

    private static final BigDecimal SZJA_RATE = new BigDecimal("0.15");
    private static final BigDecimal TB_RATE = new BigDecimal("0.185");

    private static final BigDecimal ONE_DEPENDENT_BASE = new BigDecimal("133340");
    private static final BigDecimal TWO_DEPENDENTS_BASE = new BigDecimal("266660");
    private static final BigDecimal THREE_OR_MORE_DEPENDENTS_BASE = new BigDecimal("440000");

    public SalaryNetDTO calculate(BigDecimal grossMonthly, String currencyId, int dependents) {
        if (grossMonthly == null || grossMonthly.signum() <= 0) {
            return this.zero();
        }

        BigDecimal familyBase = this.familyAllowanceBase(currencyId, dependents);
        BigDecimal szjaBase = grossMonthly.subtract(familyBase).max(BigDecimal.ZERO);
        BigDecimal szja = szjaBase.multiply(SZJA_RATE);

        BigDecimal unusedAllowance = familyBase.subtract(grossMonthly).max(BigDecimal.ZERO);
        BigDecimal tbGross = grossMonthly.multiply(TB_RATE);
        BigDecimal tbCredit = unusedAllowance.multiply(SZJA_RATE).min(tbGross);
        BigDecimal tb = tbGross.subtract(tbCredit);

        BigDecimal szjaRounded = this.round(szja, currencyId);
        BigDecimal tbRounded = this.round(tb, currencyId);

        return SalaryNetDTO.builder()
                .szjaMonthly(szjaRounded)
                .tbMonthly(tbRounded)
                .netMonthly(grossMonthly.subtract(szjaRounded).subtract(tbRounded))
                .familyAllowanceMonthly(familyBase)
                .familyAllowanceApplied(familyBase.signum() > 0)
                .build();
    }

    private BigDecimal familyAllowanceBase(String currencyId, int dependents) {
        if (dependents <= 0 || !HUF.equals(currencyId)) {
            return BigDecimal.ZERO;
        }
        return this.perDependentBase(dependents).multiply(BigDecimal.valueOf(dependents));
    }

    private BigDecimal perDependentBase(int dependents) {
        if (dependents == 1) {
            return ONE_DEPENDENT_BASE;
        }
        if (dependents == 2) {
            return TWO_DEPENDENTS_BASE;
        }
        return THREE_OR_MORE_DEPENDENTS_BASE;
    }

    public BigDecimal round(BigDecimal amount, String currencyId) {
        return HUF.equals(currencyId)
                ? amount.setScale(0, RoundingMode.HALF_UP)
                : amount.setScale(2, RoundingMode.HALF_UP);
    }

    private SalaryNetDTO zero() {
        return SalaryNetDTO.builder()
                .szjaMonthly(BigDecimal.ZERO)
                .tbMonthly(BigDecimal.ZERO)
                .netMonthly(BigDecimal.ZERO)
                .familyAllowanceMonthly(BigDecimal.ZERO)
                .familyAllowanceApplied(false)
                .build();
    }
}
