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
@ToString(exclude = "user")
@Table(name = "EOTM_SPENDING_CATEGORY",
        uniqueConstraints = @UniqueConstraint(name = "UK_SPENDING_CATEGORY_NAME",
                columnNames = {"user_id", "normalized_name"}))
@AllArgsConstructor
@NoArgsConstructor
@Generated
public class SpendingCategory extends AuditedEntity {

    public static final int NAME_MAX_LENGTH = 64;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = NAME_MAX_LENGTH)
    private String name;

    @Column(name = "normalized_name", nullable = false, length = NAME_MAX_LENGTH)
    private String normalizedName;

    @Enumerated(EnumType.STRING)
    @Column(name = "color", nullable = false, length = 16)
    private CategoryColor color;

    @Column(name = "display_order", nullable = false)
    private int position;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;
}
