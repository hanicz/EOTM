package eye.on.the.money.dto.out;

import eye.on.the.money.util.Generated;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
@ToString
@AllArgsConstructor
@EqualsAndHashCode
@NoArgsConstructor
@Generated
public class RSUVestTaxEventReportDTO {

    private List<RSUVestTaxEventDTO> items;
    private BigDecimal totalAmountInHuf;
    private TaxBreakdownDTO totalTax;
    private String error;

    public static RSUVestTaxEventReportDTO empty() {
        return RSUVestTaxEventReportDTO.builder().items(List.of())
                .totalAmountInHuf(BigDecimal.ZERO).totalTax(TaxBreakdownDTO.zero()).build();
    }
}
