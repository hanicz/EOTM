package eye.on.the.money.model.tax;

import com.fasterxml.jackson.annotation.JsonIgnore;
import eye.on.the.money.model.forex.Currency;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import javax.persistence.*;
import java.util.Date;

@Entity
@Getter
@Setter
@Builder
@Slf4j
@ToString
@Table(name = "EOTM_MNB_RATE")
@AllArgsConstructor
@NoArgsConstructor
public class MNBRate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Double rate;
    private Date rateDate;

    @ManyToOne
    @JoinColumn(name = "currency_id", nullable = false)
    @JsonIgnore
    private Currency currency;
}
