package eye.on.the.money.model.stock;

import com.fasterxml.jackson.annotation.JsonIgnore;
import eye.on.the.money.model.User;
import eye.on.the.money.util.Generated;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@Builder
@ToString(exclude = "user")
@Table(name = "EOTM_RSU_GRANT", indexes = {
        @Index(name = "IDX_RSU_GRANT_USER", columnList = "user_id")})
@AllArgsConstructor
@NoArgsConstructor
@Generated
public class RSUGrant {

    public static final int SHORT_NAME_MAX_LENGTH = 32;
    public static final int EXCHANGE_MAX_LENGTH = 16;
    public static final int CURRENCY_MAX_LENGTH = 8;
    public static final int NOTE_MAX_LENGTH = 64;
    public static final int MAX_VESTING_YEARS = 10;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "short_name", nullable = false, length = SHORT_NAME_MAX_LENGTH)
    private String shortName;

    @Column(name = "exchange", nullable = false, length = EXCHANGE_MAX_LENGTH)
    private String exchange;

    @Column(name = "currency", length = CURRENCY_MAX_LENGTH)
    private String currency;

    @Column(name = "grant_date", nullable = false)
    private LocalDate grantDate;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "vesting_years", nullable = false)
    private int vestingYears;

    @Enumerated(EnumType.STRING)
    @Column(name = "vesting_frequency", nullable = false, length = 16)
    private VestingFrequency vestingFrequency;

    @Column(name = "note", length = NOTE_MAX_LENGTH)
    private String note;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;
}
