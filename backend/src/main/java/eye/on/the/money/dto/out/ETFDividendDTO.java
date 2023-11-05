package eye.on.the.money.dto.out;

import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;

@Getter
@Setter
@Builder
@Slf4j
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class ETFDividendDTO {
    private Long id;
    private Double amount;
    private Date dividendDate;
    private String shortName;
    private String currencyId;
    private String exchange;
}
