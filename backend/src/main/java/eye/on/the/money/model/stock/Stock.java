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
@Table(name = "EOTM_STOCK")
@AllArgsConstructor
@NoArgsConstructor
@Generated
public class Stock {
    @Id
    private String id;
    private String name;
    private String shortName;
    private String exchange;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "stock")
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<Investment> investment;
}
