package eye.on.the.money.model.stock;

import com.fasterxml.jackson.annotation.JsonIgnore;
import eye.on.the.money.model.User;
import eye.on.the.money.util.Generated;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Getter
@Setter
@Builder
@ToString(exclude = "user")
@Table(name = "EOTM_STAR_GRANT", indexes = {
        @Index(name = "IDX_STAR_GRANT_USER", columnList = "user_id")})
@AllArgsConstructor
@NoArgsConstructor
@Generated
public class STARGrant {

    public static final int NAME_MAX_LENGTH = 32;
    public static final int CURRENCY_MAX_LENGTH = 8;
    public static final int NOTE_MAX_LENGTH = 64;
    public static final int MAX_VESTING_YEARS = 10;
    public static final int VALUE_PRECISION = 19;
    public static final int VALUE_SCALE = 4;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = NAME_MAX_LENGTH)
    private String name;

    @Column(name = "currency", nullable = false, length = CURRENCY_MAX_LENGTH)
    private String currency;

    @Column(name = "commencement_date", nullable = false)
    private LocalDate commencementDate;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "base_value", nullable = false, precision = VALUE_PRECISION, scale = VALUE_SCALE)
    private BigDecimal baseValue;

    @Column(name = "current_value", nullable = false, precision = VALUE_PRECISION, scale = VALUE_SCALE)
    private BigDecimal currentValue;

    @Column(name = "vesting_years", nullable = false)
    private int vestingYears;

    @Column(name = "note", length = NOTE_MAX_LENGTH)
    private String note;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;
}
