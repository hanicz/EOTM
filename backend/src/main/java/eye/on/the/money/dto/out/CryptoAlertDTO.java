package eye.on.the.money.dto.out;

import eye.on.the.money.util.Generated;
import lombok.*;

@Getter
@Setter
@Builder
@EqualsAndHashCode
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Generated
public class CryptoAlertDTO {

    private Long id;
    private String type;
    private Double valuePoint;
    private String symbol;
    private String name;
}
