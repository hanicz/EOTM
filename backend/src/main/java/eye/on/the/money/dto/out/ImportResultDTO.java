package eye.on.the.money.dto.out;

import lombok.*;
import lombok.extern.slf4j.Slf4j;

@Getter
@Setter
@Builder
@Slf4j
@ToString
@AllArgsConstructor
@EqualsAndHashCode
@NoArgsConstructor
public class ImportResultDTO {
    private int created;
    private int updated;
}
