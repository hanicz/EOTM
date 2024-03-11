package eye.on.the.money.controller;

import eye.on.the.money.util.Generated;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

@Getter
@Setter
@Builder
@Slf4j
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
