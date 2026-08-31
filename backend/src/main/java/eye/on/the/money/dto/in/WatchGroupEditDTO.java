package eye.on.the.money.dto.in;

import eye.on.the.money.model.watchlist.WatchGroup;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WatchGroupEditDTO(@NotBlank @Size(max = WatchGroup.NAME_MAX_LENGTH) String name) {
}
