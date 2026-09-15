package eye.on.the.money.model.financial;

import com.fasterxml.jackson.annotation.JsonIgnore;
import eye.on.the.money.model.AuditedEntity;
import eye.on.the.money.model.User;
import eye.on.the.money.util.Generated;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@ToString(exclude = {"user", "category"})
@Table(name = "EOTM_BANK_CATEGORY_RULE",
        uniqueConstraints = @UniqueConstraint(name = "UK_BANK_CATEGORY_RULE_PATTERN",
                columnNames = {"user_id", "normalized_pattern"}))
@AllArgsConstructor
@NoArgsConstructor
@Generated
public class BankCategoryRule extends AuditedEntity {

    public static final int NAME_MAX_LENGTH = 64;
    public static final int PATTERN_MAX_LENGTH = 64;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", length = NAME_MAX_LENGTH)
    private String name;

    @Column(name = "pattern", nullable = false, length = PATTERN_MAX_LENGTH)
    private String pattern;

    @Column(name = "normalized_pattern", nullable = false, length = PATTERN_MAX_LENGTH)
    private String normalizedPattern;

    @Column(name = "priority", nullable = false)
    private int priority;

    @Column(name = "active", nullable = false)
    private boolean active;

    @ManyToOne(optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private SpendingCategory category;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;
}
