package eye.on.the.money.model;

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
@Table(name = "CURRENCY")
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
