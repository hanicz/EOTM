package eye.on.the.money.model.alert;

import com.fasterxml.jackson.annotation.JsonIgnore;
import eye.on.the.money.model.User;
import eye.on.the.money.model.stock.Stock;
import eye.on.the.money.util.Generated;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import jakarta.persistence.*;

@Entity
@Getter
@Setter
@Slf4j
@Builder
@ToString
@Table(name = "EOTM_STOCK_ALERT")
@AllArgsConstructor
@NoArgsConstructor
@Generated
public class StockAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String type;
    private Double valuePoint;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    @ToString.Exclude
    private User user;

    @ManyToOne
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock;
}
