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
@Table(name = "EOTM_CONFIG")
@AllArgsConstructor
@NoArgsConstructor
@Generated
public class Config {
    @Id
    private String configKey;
    private String configValue;
}
