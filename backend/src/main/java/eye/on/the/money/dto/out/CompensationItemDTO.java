package eye.on.the.money.dto.out;

import eye.on.the.money.model.salary.CompensationAmountMode;
import eye.on.the.money.model.salary.CompensationTaxTreatment;
import eye.on.the.money.util.Generated;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@ToString
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
@Generated
public class CompensationItemDTO {

    private Long id;
    private String name;
    private CompensationAmountMode amountMode;
    private BigDecimal monthlyAmount;
    private BigDecimal percent;
    private CompensationTaxTreatment taxTreatment;
    private String note;
    private String currencyId;

    private BigDecimal grossMonthly;
    private BigDecimal grossAnnual;
    private BigDecimal netMonthly;
    private BigDecimal netAnnual;
}
