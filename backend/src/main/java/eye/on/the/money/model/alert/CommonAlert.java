package eye.on.the.money.model.alert;

import com.fasterxml.jackson.annotation.JsonIgnore;
import eye.on.the.money.model.User;
import eye.on.the.money.util.Generated;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

@Getter
@Setter
@Slf4j
@Builder
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Generated
public class CommonAlert {
    private Long id;
    private Double valuePoint;
    private String type;
    private String symbolOrTicker;
    private Double actualValue;
    private Double actualChange;
    @JsonIgnore
    @ToString.Exclude
    private User user;
}
