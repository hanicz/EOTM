package eye.on.the.money.model.stock;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import eye.on.the.money.util.Generated;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

@Getter
@Setter
@Slf4j
@ToString
@Builder
@AllArgsConstructor
@EqualsAndHashCode
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Generated
public class Exchange {
    @JsonProperty("Name")
    private String name;
    @JsonProperty("Code")
    private String code;
    @JsonProperty("Currency")
    private String currency;
}
