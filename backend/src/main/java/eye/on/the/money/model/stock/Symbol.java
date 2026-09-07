package eye.on.the.money.model.stock;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import eye.on.the.money.util.Generated;
import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@ToString
@Builder
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Generated
public class Symbol implements Serializable {
    @JsonProperty("Name")
    private String name;
    @JsonProperty("Code")
    private String code;
    @JsonProperty("Type")
    private String type;
    @JsonProperty("Isin")
    private String isin;
}
