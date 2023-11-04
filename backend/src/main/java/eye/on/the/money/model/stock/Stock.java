package eye.on.the.money.model.stock;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import javax.persistence.*;
import java.util.Set;

@Entity
@Getter
@Setter
@Slf4j
@ToString
@Table(name = "EOTM_STOCK")
@AllArgsConstructor
@NoArgsConstructor
public class Stock {
    @Id
    private String id;
    private String name;
    private String shortName;
    private String exchange;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "stock")
    @JsonIgnore
    @ToString.Exclude
    private Set<Investment> investment;
}
