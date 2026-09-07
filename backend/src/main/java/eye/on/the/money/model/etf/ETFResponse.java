package eye.on.the.money.model.etf;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import eye.on.the.money.util.Generated;
import lombok.*;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Generated
public class ETFResponse {
    private String code;
    private Double close;
}
