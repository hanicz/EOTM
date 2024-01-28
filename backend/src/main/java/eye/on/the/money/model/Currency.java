package eye.on.the.money.model;


import eye.on.the.money.util.Generated;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

@Entity
@Getter
@Setter
@Slf4j
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
