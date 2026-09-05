package eye.on.the.money.model.salary;

import com.fasterxml.jackson.annotation.JsonIgnore;
import eye.on.the.money.model.Currency;
import eye.on.the.money.model.User;
import eye.on.the.money.util.Generated;
import jakarta.persistence.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Getter
@Setter
@Builder
@Slf4j
@ToString(exclude = "user")
@Table(name = "EOTM_SALARY", indexes = {
        @Index(name = "IDX_SALARY_USER_FROM", columnList = "user_id, valid_from"),
        @Index(name = "IDX_SALARY_CURRENCY", columnList = "currency_id")})
@AllArgsConstructor
@NoArgsConstructor
@Generated
public class Salary {

    public static final int NOTE_MAX_LENGTH = 64;
    public static final int MAX_DEPENDENTS = 10;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "basis", nullable = false, length = 16)
    private SalaryBasis basis;

    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom;

    @Column(name = "valid_to")
    private LocalDate validTo;

    @Column(name = "dependents", nullable = false)
    private int dependents;

    @Column(name = "note", length = NOTE_MAX_LENGTH)
    private String note;

    @ManyToOne
    @JoinColumn(name = "currency_id", nullable = false)
    @JsonIgnore
    private Currency currency;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;
}
