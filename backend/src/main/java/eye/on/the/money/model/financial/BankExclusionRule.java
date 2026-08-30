package eye.on.the.money.model.financial;

import com.fasterxml.jackson.annotation.JsonIgnore;
import eye.on.the.money.model.User;
import eye.on.the.money.util.Generated;
import jakarta.persistence.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@Builder
@Slf4j
@ToString(exclude = "user")
@Table(name = "EOTM_BANK_EXCLUSION_RULE",
        uniqueConstraints = @UniqueConstraint(name = "UK_BANK_EXCLUSION_RULE_ACCOUNT",
                columnNames = {"user_id", "normalized_account", "side"}))
@AllArgsConstructor
@NoArgsConstructor
@Generated
public class BankExclusionRule {

    public static final int ACCOUNT_MAX_LENGTH = 64;
    public static final int NAME_MAX_LENGTH = 64;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", length = NAME_MAX_LENGTH)
    private String name;

    @Column(name = "account_number", nullable = false, length = ACCOUNT_MAX_LENGTH)
    private String accountNumber;

    @Column(name = "normalized_account", nullable = false, length = ACCOUNT_MAX_LENGTH)
    private String normalizedAccount;

    @Enumerated(EnumType.STRING)
    @Column(name = "side", nullable = false, length = 16)
    private AccountSide side;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "creation_date", nullable = false)
    private LocalDate creationDate;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;
}
