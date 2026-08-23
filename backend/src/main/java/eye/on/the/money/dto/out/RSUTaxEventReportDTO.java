package eye.on.the.money.dto.out;

import eye.on.the.money.util.Generated;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
@Slf4j
@ToString
@AllArgsConstructor
@EqualsAndHashCode
@NoArgsConstructor
@Generated
public class RSUTaxEventReportDTO {

    private List<RSUTaxEventDTO> items;
    private BigDecimal totalAmountInHuf;
    private TaxBreakdownDTO totalTax;

    public static RSUTaxEventReportDTO empty() {
        return RSUTaxEventReportDTO.builder().items(List.of())
                .totalAmountInHuf(BigDecimal.ZERO).totalTax(TaxBreakdownDTO.zero()).build();
    }
}
