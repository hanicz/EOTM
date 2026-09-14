package eye.on.the.money.dto.out;

import eye.on.the.money.model.financial.CategoryColor;
import eye.on.the.money.util.Generated;
import lombok.*;

@Getter
@Setter
@Builder
@ToString
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
@Generated
public class SpendingCategoryDTO {

    private Long id;
    private String name;
    private CategoryColor color;
    private int position;
}
