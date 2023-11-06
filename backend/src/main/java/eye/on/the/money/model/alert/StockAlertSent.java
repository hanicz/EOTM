package eye.on.the.money.model.alert;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import javax.persistence.*;
import java.util.Date;

@Entity
@Getter
@Setter
@Slf4j
@Builder
@ToString
@Table(name = "EOTM_STOCK_ALERT_SENT")
@AllArgsConstructor
@NoArgsConstructor
public class StockAlertSent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Date alertDate;

    @ManyToOne
    @JoinColumn(name = "alert_id", nullable = false)
    @JsonIgnore
    private StockAlert stockAlert;
}
