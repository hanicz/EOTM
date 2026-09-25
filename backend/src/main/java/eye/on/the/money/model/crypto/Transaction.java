package eye.on.the.money.model.crypto;

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
@Table(name = "EOTM_COIN_TRANSACTION", indexes = {
        @Index(name = "IDX_COIN_TRANSACTION_USER_DATE", columnList = "user_id, transaction_date"),
        @Index(name = "IDX_COIN_TRANSACTION_COIN", columnList = "coin_id"),
        @Index(name = "IDX_COIN_TRANSACTION_CURRENCY", columnList = "currency_id")})
@AllArgsConstructor
@NoArgsConstructor
@Generated
public class Transaction extends AuditedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, precision = 28, scale = 10)
    private BigDecimal quantity;

    @Column(nullable = false)
    private String buySell;

    private String transactionString;

    @Column(nullable = false)
    private LocalDate transactionDate;

    @Column(precision = 19, scale = 4)
    private BigDecimal fee;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @ManyToOne
    @JoinColumn(name = "coin_id", nullable = false)
    private Coin coin;

    @ManyToOne
    @JoinColumn(name = "currency_id", nullable = false)
    @JsonIgnore
    private Currency currency;
}
