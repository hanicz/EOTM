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
public class BankCategoryRuleDTO {

    private Long id;
    private String name;
    private String pattern;
    private Long categoryId;
    private String categoryName;
    private CategoryColor categoryColor;
    private int priority;
    private boolean active;
}
