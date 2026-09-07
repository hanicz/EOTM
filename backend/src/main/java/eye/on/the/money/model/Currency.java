package eye.on.the.money.model;


import eye.on.the.money.util.Generated;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Getter
@Setter
@ToString
@EqualsAndHashCode
@Table(name = "EOTM_CURRENCY")
@AllArgsConstructor
@NoArgsConstructor
@Generated
public class Currency {

    @Id
    private String id;
    private String name;
}
