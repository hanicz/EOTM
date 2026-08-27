package eye.on.the.money.model.etf;

import com.fasterxml.jackson.annotation.JsonIgnore;
import eye.on.the.money.util.Generated;
import jakarta.persistence.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;

@Entity
@Getter
@Setter
@Slf4j
@ToString
@EqualsAndHashCode
@Table(name = "EOTM_ETF",
        uniqueConstraints = @UniqueConstraint(name = "UK_ETF_SHORT_NAME_EXCHANGE",
                columnNames = {"short_name", "exchange"}),
        indexes = @Index(name = "IDX_ETF_SHORT_NAME", columnList = "short_name"))
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Generated
public class ETF {
    @Id
    private String id;
    private String name;

    @Column(nullable = false)
    private String shortName;

    @Column(nullable = false)
    private String exchange;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "etf")
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<ETFInvestment> etfInvestment;
}
