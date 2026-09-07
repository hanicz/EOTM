package eye.on.the.money.model.salary;

import com.fasterxml.jackson.annotation.JsonIgnore;
import eye.on.the.money.model.User;
import eye.on.the.money.util.Generated;
import jakarta.persistence.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@Builder
@Slf4j
@ToString(exclude = "user")
@Table(name = "EOTM_COMPENSATION", indexes = {
        @Index(name = "IDX_COMPENSATION_USER", columnList = "user_id")})
@AllArgsConstructor
@NoArgsConstructor
@Generated
public class CompensationItem {

    public static final int NAME_MAX_LENGTH = 64;
    public static final int NOTE_MAX_LENGTH = 64;
    public static final String MAX_PERCENT = "500";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = NAME_MAX_LENGTH)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "amount_mode", nullable = false, length = 24)
    private CompensationAmountMode amountMode;

    @Column(name = "monthly_amount", precision = 19, scale = 2)
    private BigDecimal monthlyAmount;

    @Column(name = "percent_of_annual", precision = 6, scale = 2)
    private BigDecimal percent;

    @Enumerated(EnumType.STRING)
    @Column(name = "tax_treatment", nullable = false, length = 24)
    private CompensationTaxTreatment taxTreatment;

    @Column(name = "note", length = NOTE_MAX_LENGTH)
    private String note;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;
}
