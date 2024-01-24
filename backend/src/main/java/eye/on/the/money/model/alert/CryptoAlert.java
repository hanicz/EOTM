package eye.on.the.money.model.alert;

import eye.on.the.money.model.crypto.Coin;
import eye.on.the.money.util.Generated;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

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
public class CryptoAlert extends Alert {
    @ManyToOne
    @JoinColumn(name = "coin_id", nullable = false)
    private Coin coin;

    @Override
    public String getAlertType() {
        return "crypto";
    }
}
