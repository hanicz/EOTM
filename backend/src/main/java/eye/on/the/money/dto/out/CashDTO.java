package eye.on.the.money.dto.out;

import eye.on.the.money.util.Generated;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@Setter
@Builder
@EqualsAndHashCode
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Generated
public class CashDTO {

    @NotNull
    @PositiveOrZero
    private Double amount;

    private String currency;
}
