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
@Table(name = "EOTM_CREDENTIAL")
@AllArgsConstructor
@NoArgsConstructor
@Generated
public class Credential {
    @Id
    private String name;

    @ToString.Exclude
    private String secret;
}
