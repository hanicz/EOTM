package eye.on.the.money.model.forex;

import com.fasterxml.jackson.annotation.JsonIgnore;
import eye.on.the.money.model.Currency;
import eye.on.the.money.model.User;
import eye.on.the.money.util.Generated;
import jakarta.persistence.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@Builder
@Slf4j
@ToString
@Table(name = "EOTM_FOREX_TRANSACTION", indexes = {
        @Index(name = "IDX_FOREX_TRANSACTION_USER_DATE", columnList = "user_id, transaction_date"),
        @Index(name = "IDX_FOREX_TRANSACTION_FROM_CURRENCY", columnList = "from_currency_id"),
        @Index(name = "IDX_FOREX_TRANSACTION_TO_CURRENCY", columnList = "to_currency_id")})
@AllArgsConstructor
@NoArgsConstructor
@Generated
public class ForexTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Double fromAmount;

    @Column(nullable = false)
    private Double toAmount;

    @Column(nullable = false)
    private LocalDate transactionDate;

    @Column(nullable = false)
    private String buySell;

    @Column(nullable = false)
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
