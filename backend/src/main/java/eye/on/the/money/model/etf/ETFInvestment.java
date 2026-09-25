package eye.on.the.money.model.etf;

import com.fasterxml.jackson.annotation.JsonIgnore;
import eye.on.the.money.model.AuditedEntity;
import eye.on.the.money.model.Currency;
import eye.on.the.money.model.User;
import eye.on.the.money.model.stock.Account;
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
@Table(name = "EOTM_ETF_INVESTMENT", indexes = {
        @Index(name = "IDX_ETF_INVESTMENT_USER_DATE", columnList = "user_id, transaction_date"),
        @Index(name = "IDX_ETF_INVESTMENT_ETF", columnList = "etf_id"),
        @Index(name = "IDX_ETF_INVESTMENT_ACCOUNT", columnList = "account_id"),
        @Index(name = "IDX_ETF_INVESTMENT_CURRENCY", columnList = "currency_id")})
@AllArgsConstructor
@NoArgsConstructor
@Generated
public class ETFInvestment extends AuditedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, precision = 28, scale = 10)
    private BigDecimal quantity;

    @Column(nullable = false)
    private String buySell;

    @Column(nullable = false)
    private LocalDate transactionDate;

    @Column(precision = 19, scale = 4)
    private BigDecimal fee;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    @ToString.Exclude
    private User user;

    @ManyToOne
    @JoinColumn(name = "etf_id", nullable = false)
    private ETF etf;

    @ManyToOne
    @JoinColumn(name = "currency_id", nullable = false)
    @JsonIgnore
    private Currency currency;

    @ManyToOne
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;
}
