package eye.on.the.money.dto.in;

import eye.on.the.money.model.stock.STARGrant;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record STARGrantEditDTO(@NotBlank @Size(max = STARGrant.NAME_MAX_LENGTH) String name,
                               @NotBlank @Size(max = STARGrant.CURRENCY_MAX_LENGTH) String currency,
                               @NotNull LocalDate commencementDate,
                               @NotNull @Positive Integer quantity,
                               @NotNull @PositiveOrZero BigDecimal baseValue,
                               @NotNull @PositiveOrZero BigDecimal currentValue,
                               @NotNull @Min(1) @Max(STARGrant.MAX_VESTING_YEARS) Integer vestingYears,
                               @Size(max = STARGrant.NOTE_MAX_LENGTH) String note,
                               boolean applyValueToAll) {
}
