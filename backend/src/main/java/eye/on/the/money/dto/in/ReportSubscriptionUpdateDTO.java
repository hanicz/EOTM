package eye.on.the.money.dto.in;

import eye.on.the.money.model.report.ReportSubscription;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ReportSubscriptionUpdateDTO(
        @NotNull Boolean enabled,
        @NotBlank @Size(min = 3, max = 3) String currency,
        @Size(max = ReportSubscription.MAX_RECIPIENTS) List<@Email @Size(max = 255) String> recipients) {
}
