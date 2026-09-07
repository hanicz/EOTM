package eye.on.the.money.dto.in;

import eye.on.the.money.model.stock.RSUGrant;
import eye.on.the.money.model.stock.VestingFrequency;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record RSUGrantEditDTO(@NotBlank @Size(max = RSUGrant.SHORT_NAME_MAX_LENGTH) String shortName,
                              @Size(max = RSUGrant.EXCHANGE_MAX_LENGTH) String exchange,
                              @Size(max = RSUGrant.CURRENCY_MAX_LENGTH) String currency,
                              @NotNull LocalDate grantDate,
                              @NotNull @Positive Integer quantity,
                              @NotNull @Min(1) @Max(RSUGrant.MAX_VESTING_YEARS) Integer vestingYears,
                              @NotNull VestingFrequency vestingFrequency,
                              @Size(max = RSUGrant.NOTE_MAX_LENGTH) String note) {
}
