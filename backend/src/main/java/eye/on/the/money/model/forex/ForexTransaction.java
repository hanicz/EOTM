package eye.on.the.money.model.forex;

import com.fasterxml.jackson.annotation.JsonIgnore;
import eye.on.the.money.model.Currency;
import eye.on.the.money.model.User;
import eye.on.the.money.util.Generated;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import jakarta.persistence.*;
import java.util.Date;

@Entity
@Getter
@Setter
@Builder
@Slf4j
@ToString
@Table(name = "EOTM_FOREX_TRANSACTION")
@AllArgsConstructor
@NoArgsConstructor
@Generated
public class ForexTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Double fromAmount;
    private Double toAmount;
    private Date transactionDate;
    private String buySell;
    private Double changeRate;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @ManyToOne
    @JoinColumn(name = "from_currency_id", nullable = false)
    private Currency fromCurrency;

    @ManyToOne
    @JoinColumn(name = "to_currency_id", nullable = false)
    private Currency toCurrency;
}
