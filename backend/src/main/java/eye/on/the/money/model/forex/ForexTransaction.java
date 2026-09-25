package eye.on.the.money.model.forex;

import com.fasterxml.jackson.annotation.JsonIgnore;
import eye.on.the.money.model.AuditedEntity;
import eye.on.the.money.model.Currency;
import eye.on.the.money.model.User;
import eye.on.the.money.util.Generated;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Getter
@Setter
@Builder
@ToString
@Table(name = "EOTM_FOREX_TRANSACTION", indexes = {
        @Index(name = "IDX_FOREX_TRANSACTION_USER_DATE", columnList = "user_id, transaction_date"),
        @Index(name = "IDX_FOREX_TRANSACTION_FROM_CURRENCY", columnList = "from_currency_id"),
        @Index(name = "IDX_FOREX_TRANSACTION_TO_CURRENCY", columnList = "to_currency_id")})
@AllArgsConstructor
@NoArgsConstructor
@Generated
public class ForexTransaction extends AuditedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal fromAmount;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal toAmount;

    @Column(nullable = false)
    private LocalDate transactionDate;

    @Column(nullable = false)
    private String buySell;

    @Column(nullable = false, precision = 19, scale = 8)
    private BigDecimal changeRate;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    @ToString.Exclude
    private User user;

    @ManyToOne
    @JoinColumn(name = "from_currency_id", nullable = false)
    private Currency fromCurrency;

    @ManyToOne
    @JoinColumn(name = "to_currency_id", nullable = false)
    private Currency toCurrency;
}
