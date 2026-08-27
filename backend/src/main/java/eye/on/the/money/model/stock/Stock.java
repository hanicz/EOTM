package eye.on.the.money.model.stock;

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
@Builder
@EqualsAndHashCode
@ToString
@Table(name = "EOTM_STOCK",
        uniqueConstraints = @UniqueConstraint(name = "UK_STOCK_SHORT_NAME_EXCHANGE",
                columnNames = {"short_name", "exchange"}),
        indexes = @Index(name = "IDX_STOCK_SHORT_NAME", columnList = "short_name"))
@AllArgsConstructor
@NoArgsConstructor
@Generated
public class Stock {
    @Id
    private String id;
    private String name;

    @Column(nullable = false)
    private String shortName;

    @Column(nullable = false)
    private String exchange;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "stock")
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<Investment> investment;
}
