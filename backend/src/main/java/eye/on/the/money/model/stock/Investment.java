package eye.on.the.money.model.stock;

import com.fasterxml.jackson.annotation.JsonIgnore;
import eye.on.the.money.model.User;
import eye.on.the.money.util.Generated;
import jakarta.persistence.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@Builder
@Slf4j
@ToString
@Table(name = "EOTM_STOCK_INVESTMENT", indexes = {
        @Index(name = "IDX_STOCK_INVESTMENT_USER_DATE", columnList = "user_id, transaction_date"),
        @Index(name = "IDX_STOCK_INVESTMENT_USER_RSU", columnList = "user_id, rsu"),
        @Index(name = "IDX_STOCK_INVESTMENT_STOCK", columnList = "stock_id"),
        @Index(name = "IDX_STOCK_INVESTMENT_ACCOUNT", columnList = "account_id")})
@AllArgsConstructor
@NoArgsConstructor
@Generated
public class Investment {

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

    @Column(name = "rsu", nullable = false)
    @ColumnDefault("false")
    private boolean rsu;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @ManyToOne
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock;

    @OneToOne(cascade = CascadeType.REMOVE, orphanRemoval = true)
    @JoinColumn(name = "stockPayment_id", nullable = false)
    private StockPayment stockPayment;

    @ManyToOne
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;
}
