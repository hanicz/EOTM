package eye.on.the.money.model.crypto;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
@Table(name = "EOTM_COIN_TRANSACTION", indexes = {
        @Index(name = "IDX_COIN_TRANSACTION_USER_DATE", columnList = "user_id, transaction_date"),
        @Index(name = "IDX_COIN_TRANSACTION_COIN", columnList = "coin_id")})
@AllArgsConstructor
@NoArgsConstructor
@Generated
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Double quantity;

    @Column(nullable = false)
    private String buySell;

    @Column(nullable = false)
    private LocalDate creationDate;

    private String transactionString;

    @Column(nullable = false)
    private LocalDate transactionDate;

    private Double fee;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @ManyToOne
    @JoinColumn(name = "coin_id", nullable = false)
    private Coin coin;

    @OneToOne(cascade = CascadeType.REMOVE, orphanRemoval = true)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;
}
