package eye.on.the.money.model.crypto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import eye.on.the.money.model.forex.Currency;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import javax.persistence.*;

@Entity
@Data
@Builder
@Slf4j
@ToString
@Table(name = "EOTM_COIN_PAYMENT")
@AllArgsConstructor
@NoArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Double amount;

    @OneToOne(mappedBy = "payment")
    @JsonIgnore
    private Transaction transaction;

    @ManyToOne
    @JoinColumn(name = "currency_id", nullable = false)
    @JsonIgnore
    private Currency currency;
}
