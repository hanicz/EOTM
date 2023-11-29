package eye.on.the.money.model.stock;

import eye.on.the.money.util.Generated;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.List;

@Getter
@Setter
@Slf4j
@Builder
@EqualsAndHashCode
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Generated
public class Profile {
    private String country;
    private String currency;
    private String exchange;
    private String finnhubIndustry;
    private Date ipo;
    private String logo;
    private String name;
    private String ticker;
    private String weburl;
    private Double marketCapitalization;
    private Long shareOutstanding;
    private List<String> peers;
}
