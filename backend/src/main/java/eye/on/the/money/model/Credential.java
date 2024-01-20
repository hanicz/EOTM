package eye.on.the.money.model;

import eye.on.the.money.util.Generated;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import jakarta.persistence.*;

@Entity
@Getter
@Setter
@Slf4j
@ToString
@Table(name = "EOTM_CREDENTIAL")
@AllArgsConstructor
@NoArgsConstructor
@Generated
public class Credential {
    @Id
    private String name;
    private String secret;
}
