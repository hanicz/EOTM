package eye.on.the.money.dto.out;

import eye.on.the.money.util.Generated;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
@EqualsAndHashCode
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Generated
public class TaxReportDTO {

    private List<RSUTaxDTO> items;
    private BigDecimal totalAmountInHuf;
    private TaxBreakdownDTO totalTax;
}
