package eye.on.the.money.dto.out;

import eye.on.the.money.util.Generated;
import lombok.*;

import java.io.Serializable;
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
public class STARGrantReportDTO implements Serializable {

    private List<STARGrantDTO> items;
    private BigDecimal totalAmountInHuf;
    private TaxBreakdownDTO totalTax;
    private BigDecimal totalNetInHuf;

    public static STARGrantReportDTO empty() {
        return STARGrantReportDTO.builder().items(List.of()).totalAmountInHuf(BigDecimal.ZERO)
                .totalTax(TaxBreakdownDTO.zero()).totalNetInHuf(BigDecimal.ZERO).build();
    }
}
