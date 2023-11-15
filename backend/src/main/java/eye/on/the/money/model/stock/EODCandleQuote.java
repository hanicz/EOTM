package eye.on.the.money.model.stock;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;

@Slf4j
@Getter
@Setter
@Builder
@EqualsAndHashCode
@ToString
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class EODCandleQuote {

    private Double close;
    private Double high;
    private Double low;
    private Double open;
    private Date date;
    private Long volume;

    public boolean sameSize() {
        return true;
    }
}
