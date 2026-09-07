package eye.on.the.money.dto.out;

import lombok.*;

@Getter
@Setter
@Builder
@ToString
@AllArgsConstructor
@EqualsAndHashCode
@NoArgsConstructor
public class ImportResultDTO {
    private int created;
    private int updated;
}
