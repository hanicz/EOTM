package eye.on.the.money.model.stock;

import com.fasterxml.jackson.annotation.JsonIgnore;
import eye.on.the.money.model.User;
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
@Table(name = "EOTM_STOCK_INVESTMENT")
@AllArgsConstructor
@NoArgsConstructor
public class Investment {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private Double quantity;
    private String buySell;
    private Date creationDate;
    private Date transactionDate;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @ManyToOne
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock;

    @OneToOne
    @JoinColumn(name = "stockpayment_id", nullable = false)
    private StockPayment stockPayment;
}
