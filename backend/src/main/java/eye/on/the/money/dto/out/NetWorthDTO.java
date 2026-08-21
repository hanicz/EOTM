package eye.on.the.money.dto.out;

import eye.on.the.money.util.Generated;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.List;

/**
 * Everything a user holds, valued in a single currency.
 */
@Slf4j
@Getter
@Setter
@Builder
@EqualsAndHashCode
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Generated
public class NetWorthDTO {

    private String currency;

    private BigDecimal totalSpent;
    private BigDecimal totalWorth;
    private BigDecimal totalChangePct;

    private List<AssetClassValueDTO> assets;

    /** Every currency the user actually holds something in, so the UI can offer them in a picker. */
    private List<String> availableCurrencies;

    /**
     * Currencies that had holdings but no published rate against the target, so their value is missing from
     * the totals. Empty in the normal case; surfaced so the UI can say the figure is incomplete rather than
     * quietly showing a number that is too low.
     */
    private List<String> unconvertedCurrencies;
}
