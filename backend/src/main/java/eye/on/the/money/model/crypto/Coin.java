package eye.on.the.money.model.crypto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.FetchType;
import java.util.Set;

@Entity
@Getter
@Setter
@Slf4j
@ToString
@Table(name = "EOTM_COIN")
@AllArgsConstructor
@NoArgsConstructor
public class Coin {

    @Id
    private String id;
    private String name;
    private String symbol;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "coin")
    @JsonIgnore
    @ToString.Exclude
    private Set<Transaction> transaction;
}