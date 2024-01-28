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
@Table(name = "EOTM_ETF")
@AllArgsConstructor
@NoArgsConstructor
@Generated
public class ETF {
    @Id
    private String id;
    private String name;
    private String shortName;
    private String exchange;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "etf")
    @JsonIgnore
    @ToString.Exclude
    private Set<ETFInvestment> etfInvestment;
}
