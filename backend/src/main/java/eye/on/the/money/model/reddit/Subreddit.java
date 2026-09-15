package eye.on.the.money.model.reddit;

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
@ToString
@Table(name = "EOTM_SUBREDDIT", indexes = @Index(name = "IDX_SUBREDDIT_USER", columnList = "user_id"))
@AllArgsConstructor
@NoArgsConstructor
@Generated
@EqualsAndHashCode(callSuper = false)
public class Subreddit extends AuditedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String subreddit;
    private String description;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;
}
