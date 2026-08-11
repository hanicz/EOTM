package eye.on.the.money.service.shared;

import eye.on.the.money.dto.out.TaxBreakdownDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
@Slf4j
public class TaxCalculator {

    private static final BigDecimal BASE_MULTIPLIER = new BigDecimal("0.89");
    private static final BigDecimal SZOCHO_RATE = new BigDecimal("0.13");
    private static final BigDecimal SZJA_RATE = new BigDecimal("0.15");

    public TaxBreakdownDTO calculateTax(BigDecimal amountInHuf) {
        if (amountInHuf == null || amountInHuf.signum() <= 0) return TaxBreakdownDTO.zero();

        BigDecimal taxBase = amountInHuf.multiply(BASE_MULTIPLIER);
        BigDecimal szocho = taxBase.multiply(SZOCHO_RATE).setScale(0, RoundingMode.HALF_UP);
        BigDecimal szja = taxBase.multiply(SZJA_RATE).setScale(0, RoundingMode.HALF_UP);

        return TaxBreakdownDTO.builder()
                .amount(amountInHuf.setScale(2, RoundingMode.HALF_UP))
                .taxBase(taxBase.setScale(2, RoundingMode.HALF_UP))
                .szocho(szocho)
                .szja(szja)
                .total(szocho.add(szja))
                .build();
    }
}
