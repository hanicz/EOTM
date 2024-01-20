package eye.on.the.money.model.crypto;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
@Table(name = "EOTM_COIN_TRANSACTION")
@AllArgsConstructor
@NoArgsConstructor
@Generated
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Double quantity;
    private String buySell;
    private Date creationDate;
    private String transactionString;
    private Date transactionDate;
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
