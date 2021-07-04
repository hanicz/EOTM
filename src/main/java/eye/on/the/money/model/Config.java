package eye.on.the.money.model;

import lombok.*;
import lombok.extern.slf4j.Slf4j;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Getter
@Setter
@Slf4j
@ToString
@Table(name = "EOTM_CONFIG")
@AllArgsConstructor
@NoArgsConstructor
public class Config {
    @Id
    private String configKey;
    private String configValue;
}
