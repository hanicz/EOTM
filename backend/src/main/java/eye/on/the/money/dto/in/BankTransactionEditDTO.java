package eye.on.the.money.dto.in;

import eye.on.the.money.model.financial.BankTransaction;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record BankTransactionEditDTO(@NotNull LocalDate bookingDate,
                                     @NotNull @Size(max = BankTransaction.MEMO_MAX_LENGTH) String memo) {
}
