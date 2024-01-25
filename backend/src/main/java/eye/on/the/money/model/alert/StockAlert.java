package eye.on.the.money.model.alert;

import eye.on.the.money.model.stock.Stock;
import eye.on.the.money.util.Generated;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.*;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

@Entity
@Getter
@Setter
@Slf4j
@SuperBuilder
@ToString
@Table(name = "EOTM_STOCK_ALERT")
@AllArgsConstructor
@NoArgsConstructor
@Generated
public class StockAlert extends Alert {
    @ManyToOne
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock;
}
