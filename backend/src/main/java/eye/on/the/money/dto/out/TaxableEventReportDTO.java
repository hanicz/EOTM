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
public class TaxableEventReportDTO {

    private List<TaxableEventDTO> items;
    private BigDecimal totalAmountInHuf;
    private TaxBreakdownDTO totalTax;

    public static TaxableEventReportDTO empty() {
        return TaxableEventReportDTO.builder().items(List.of())
                .totalAmountInHuf(BigDecimal.ZERO).totalTax(TaxBreakdownDTO.zero()).build();
    }
}
