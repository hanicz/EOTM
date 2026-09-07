package eye.on.the.money.model.cash;

import com.fasterxml.jackson.annotation.JsonIgnore;
import eye.on.the.money.model.Currency;
import eye.on.the.money.model.User;
import eye.on.the.money.util.Generated;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@ToString
@Table(name = "EOTM_CASH",
        uniqueConstraints = @UniqueConstraint(name = "UK_EOTM_CASH_USER", columnNames = "user_id"))
@AllArgsConstructor
@NoArgsConstructor
@Generated
public class Cash {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Double amount;

    @ManyToOne
    @JoinColumn(name = "currency_id", nullable = false)
    @JsonIgnore
    private Currency currency;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;
}
