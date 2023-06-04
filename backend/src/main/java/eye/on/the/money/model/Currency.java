package eye.on.the.money.model;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.persistence.*;

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
}
