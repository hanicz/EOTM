package eye.on.the.money.dto.out;

import eye.on.the.money.util.Generated;
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
@Generated
public class ETFDividendDTO {
    private Long id;
    private Double amount;
    private Date dividendDate;
    private String shortName;
    private String currencyId;
    private String exchange;
}
