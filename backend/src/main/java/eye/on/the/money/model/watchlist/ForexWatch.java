package eye.on.the.money.model.watchlist;

import com.fasterxml.jackson.annotation.JsonIgnore;
import eye.on.the.money.model.Currency;
import eye.on.the.money.model.User;
import eye.on.the.money.util.Generated;
import jakarta.persistence.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

@Entity
@Getter
@Setter
@Slf4j
@ToString
@Table(name = "EOTM_FOREX_WATCH")
@AllArgsConstructor
@NoArgsConstructor
@Generated
public class ForexWatch {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @ManyToOne
    @JoinColumn(name = "currency_id_from", nullable = false)
    private Currency fromCurrency;

    @ManyToOne
    @JoinColumn(name = "currency_id", nullable = false)
    private Currency toCurrency;
}
