package eye.on.the.money.model.stock;

import com.fasterxml.jackson.annotation.JsonIgnore;
import eye.on.the.money.model.Currency;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import javax.persistence.*;

@Entity
@Data
@Builder
@Slf4j
@ToString
@Table(name = "EOTM_STOCK_PAYMENT")
@AllArgsConstructor
@NoArgsConstructor
public class StockPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private Double amount;

    @OneToOne(mappedBy = "investment")
    @JsonIgnore
    private Investment investment;

    @ManyToOne
    @JoinColumn(name = "currency_id", nullable = false)
    @JsonIgnore
    private Currency currency;
}
