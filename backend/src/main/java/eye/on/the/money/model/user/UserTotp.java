package eye.on.the.money.model.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import eye.on.the.money.model.AuditedEntity;
import eye.on.the.money.model.User;
import eye.on.the.money.util.Generated;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@SuperBuilder
@ToString(exclude = {"user", "secret"})
@Table(name = "EOTM_USER_TOTP",
        uniqueConstraints = @UniqueConstraint(name = "UK_EOTM_USER_TOTP_USER", columnNames = "user_id"))
@AllArgsConstructor
@NoArgsConstructor
@Generated
public class UserTotp extends AuditedEntity {

    public static final int MAX_SECRET_LENGTH = 512;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @Column(name = "secret", nullable = false, length = MAX_SECRET_LENGTH)
    @JsonIgnore
    private String secret;

    @Column(name = "confirmed", nullable = false)
    @ColumnDefault("false")
    private boolean confirmed;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "last_time_step")
    private Long lastTimeStep;
}
