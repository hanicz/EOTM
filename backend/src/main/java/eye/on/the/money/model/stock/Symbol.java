package eye.on.the.money.model.stock;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import eye.on.the.money.util.Generated;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;

@Getter
@Setter
@Slf4j
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
}
