package eye.on.the.money.model.alert;

import com.fasterxml.jackson.annotation.JsonIgnore;
import eye.on.the.money.model.User;
import eye.on.the.money.model.crypto.Coin;
import eye.on.the.money.util.Generated;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import jakarta.persistence.*;

@Entity
@Getter
@Setter
@Slf4j
@Builder
@ToString
@Table(name = "EOTM_COIN_ALERT")
@AllArgsConstructor
@NoArgsConstructor
@Generated
public class CryptoAlert {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String type;
    private Double valuePoint;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    @ToString.Exclude
    private User user;

    @ManyToOne
    @JoinColumn(name = "coin_id", nullable = false)
    private Coin coin;
}
