package eye.on.the.money.model.crypto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import eye.on.the.money.model.Payment;
import eye.on.the.money.model.User;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import javax.persistence.*;
import java.util.Date;

@Entity
@Getter
@Setter
@Slf4j
@ToString
@Table(name = "COIN_TRANSACTION")
@AllArgsConstructor
@NoArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private Double quantity;
    private String buySell;
    private Date creationDate;
    private String transactionString;
    private Date transactionDate;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @ManyToOne
    @JoinColumn(name = "coin_id", nullable = false)
    private Coin coin;

    @OneToOne
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;
}
