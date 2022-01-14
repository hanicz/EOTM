package eye.on.the.money.model.etf;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import javax.persistence.*;
import java.util.Date;
import java.util.Set;

@Entity
@Getter
@Setter
@Slf4j
@ToString
@Table(name = "EOTM_ETF")
@AllArgsConstructor
@NoArgsConstructor
public class ETF {
    @Id
    private String id;
    private String name;
    private String shortName;
    private String exchange;
    private Double liveValue;
    private Date eodDate;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "etf")
    @JsonIgnore
    @ToString.Exclude
    private Set<ETFInvestment> etfInvestment;
}
