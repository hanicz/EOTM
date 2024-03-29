package eye.on.the.money.model.crypto;

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
@Builder
@Table(name = "EOTM_COIN")
@AllArgsConstructor
@EqualsAndHashCode
@NoArgsConstructor
@Generated
public class Coin {

    @Id
    private String id;
    private String name;
    private String symbol;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "coin")
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<Transaction> transaction;
}