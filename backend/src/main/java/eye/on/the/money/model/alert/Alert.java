package eye.on.the.money.model.alert;

import com.fasterxml.jackson.annotation.JsonIgnore;
import eye.on.the.money.model.User;
import eye.on.the.money.util.Generated;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

@Getter
@Setter
@Slf4j
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Generated
@SuperBuilder
@MappedSuperclass
public abstract class Alert {
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

    private transient String symbolOrTicker;
    private transient Double actualValue;
    private transient Double actualChange;

    public boolean isAlertActive() {
        switch (this.getType()) {
            case "PERCENT_OVER":
                if (this.getActualChange() >= this.getValuePoint()) {
                    return true;
                }
                break;
            case "PERCENT_UNDER":
                if (this.getActualChange() <= this.getValuePoint()) {
                    return true;
                }
                break;
            case "PRICE_OVER":
                if (this.getActualValue() >= this.getValuePoint()) {
                    return true;
                }
                break;
            case "PRICE_UNDER":
                if (this.getActualValue() <= this.getValuePoint()) {
                    return true;
                }
                break;
            default:
                break;
        }
        return false;
    }

    public abstract String getAlertType();

}
