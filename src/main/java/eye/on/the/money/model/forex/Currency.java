package eye.on.the.money.model.forex;

import com.fasterxml.jackson.annotation.JsonIgnore;
import eye.on.the.money.model.crypto.Payment;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.persistence.*;
import java.util.Set;

@Entity
@Getter
@Setter
@Slf4j
@ToString
@Table(name = "EOTM_CURRENCY")
@AllArgsConstructor
@NoArgsConstructor
public class Currency {

    @Id
    private String id;
    private String name;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "currency")
    @JsonIgnore
    @ToString.Exclude
    private Set<Payment> payment;
}
