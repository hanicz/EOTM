package eye.on.the.money.dto.out;

import eye.on.the.money.util.Generated;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;

@Slf4j
@Getter
@Setter
@Builder
@EqualsAndHashCode
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Generated
public class TaxBreakdownDTO {

    /** The amount the tax was worked out from. */
    private BigDecimal amount;
    /** 89% of the amount - the base both taxes are charged on. */
    private BigDecimal taxBase;
    private BigDecimal szocho;
    private BigDecimal szja;
    private BigDecimal total;

    public TaxBreakdownDTO plus(TaxBreakdownDTO other) {
        return TaxBreakdownDTO.builder()
                .amount(this.amount.add(other.amount))
                .taxBase(this.taxBase.add(other.taxBase))
                .szocho(this.szocho.add(other.szocho))
                .szja(this.szja.add(other.szja))
                .total(this.total.add(other.total))
                .build();
    }

    public static TaxBreakdownDTO zero() {
        return TaxBreakdownDTO.builder()
                .amount(BigDecimal.ZERO).taxBase(BigDecimal.ZERO)
                .szocho(BigDecimal.ZERO).szja(BigDecimal.ZERO).total(BigDecimal.ZERO)
                .build();
    }
}
