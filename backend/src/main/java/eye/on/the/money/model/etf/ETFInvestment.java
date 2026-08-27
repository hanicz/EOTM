package eye.on.the.money.model.etf;

import com.fasterxml.jackson.annotation.JsonIgnore;
import eye.on.the.money.model.Currency;
import eye.on.the.money.model.User;
import eye.on.the.money.model.stock.Account;
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
@Table(name = "EOTM_ETF_INVESTMENT", indexes = {
        @Index(name = "IDX_ETF_INVESTMENT_USER_DATE", columnList = "user_id, transaction_date"),
        @Index(name = "IDX_ETF_INVESTMENT_ETF", columnList = "etf_id"),
        @Index(name = "IDX_ETF_INVESTMENT_ACCOUNT", columnList = "account_id"),
        @Index(name = "IDX_ETF_INVESTMENT_CURRENCY", columnList = "currency_id")})
@AllArgsConstructor
@NoArgsConstructor
@Generated
public class ETFInvestment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private String buySell;

    @Column(nullable = false)
    private LocalDate creationDate;

    @Column(nullable = false)
    private LocalDate transactionDate;

    private Double fee;

    @Column(nullable = false)
    private Double amount;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
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
