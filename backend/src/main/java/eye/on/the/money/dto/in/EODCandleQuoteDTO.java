package eye.on.the.money.dto.in;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import eye.on.the.money.util.Generated;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@EqualsAndHashCode
@ToString
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Generated
public class EODCandleQuoteDTO {

    private Double close;
    private Double high;
    private Double low;
    private Double open;
    private LocalDate date;
    private Long volume;
}
