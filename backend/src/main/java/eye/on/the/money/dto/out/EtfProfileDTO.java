package eye.on.the.money.dto.out;

import eye.on.the.money.util.Generated;
import lombok.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@EqualsAndHashCode
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Generated
public class EtfProfileDTO implements Serializable {
    private String isin;
    private String provider;
    private String indexName;
    private String investmentFocus;
    private String fundSize;
    private String ter;
    private String replication;
    private String distributionPolicy;
    private String fundCurrency;
    private String currencyHedge;
    private String inceptionDate;
    private String volatility;
    private Integer totalHoldings;
    @Builder.Default
    private List<EtfWeightDTO> holdings = new ArrayList<>();
    @Builder.Default
    private List<EtfWeightDTO> countries = new ArrayList<>();
    @Builder.Default
    private List<EtfWeightDTO> sectors = new ArrayList<>();
}
