package eye.on.the.money.dto.out;

import eye.on.the.money.util.Generated;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Getter
@Setter
@Builder
@Slf4j
@EqualsAndHashCode
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Generated
public class DashboardRatesDTO {

    private Map<String, Double> rates;
}
