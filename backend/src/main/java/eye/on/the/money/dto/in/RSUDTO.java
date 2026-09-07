package eye.on.the.money.dto.in;

import eye.on.the.money.util.Generated;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@EqualsAndHashCode
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Generated
public class RSUDTO {

    @NotNull
    private String shortName;

    /** EODHD exchange code, defaulted to US. */
    private String exchange;

    /** Overrides the currency implied by the exchange; leave unset to look it up. */
    private String currency;

    @NotNull
    private LocalDate date;

    @NotNull
    @Positive
    private Integer quantity;
}
