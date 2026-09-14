package eye.on.the.money.dto.out;

import com.fasterxml.jackson.annotation.JsonIgnore;
import eye.on.the.money.dto.CSVHelper;
import eye.on.the.money.model.financial.CategoryColor;
import java.math.BigDecimal;
import java.math.RoundingMode;
import lombok.*;

@Getter
@Setter
@Builder
@ToString
@AllArgsConstructor
@EqualsAndHashCode
@NoArgsConstructor
public class MonthlyCategorySpendingDTO implements CSVHelper {

    public static final String UNCATEGORIZED = "Uncategorized";

    private Integer year;
    private Integer month;
    private String currencyId;
    private Long categoryId;
    private String categoryName;
    private CategoryColor categoryColor;
    private Double amount;
    private Long transactionCount;

    public String getCategoryName() {
        return this.categoryName == null ? UNCATEGORIZED : this.categoryName;
    }

    public Double getAmount() {
        return this.amount == null ? null
                : BigDecimal.valueOf(this.amount).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    @Override
    @JsonIgnore
    public Object[] getHeaders() {
        return new String[]{"Year", "Month", "Currency", "Category", "Amount", "Transactions"};
    }

    @Override
    @JsonIgnore
    public Object[] getCSVRecord() {
        return new Object[]{this.getYear(), this.getMonth(), this.getCurrencyId(), this.getCategoryName(),
                this.getAmount(), this.getTransactionCount()};
    }
}
