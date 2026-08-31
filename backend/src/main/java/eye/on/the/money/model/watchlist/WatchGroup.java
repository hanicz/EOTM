package eye.on.the.money.model.watchlist;

import com.fasterxml.jackson.annotation.JsonIgnore;
import eye.on.the.money.model.User;
import eye.on.the.money.util.Generated;
import jakarta.persistence.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;

@Entity
@Getter
@Setter
@Slf4j
@Builder
@ToString
@EqualsAndHashCode
@Table(name = "EOTM_WATCH_GROUP",
        uniqueConstraints = @UniqueConstraint(name = "UK_WATCH_GROUP_USER_NAME",
                columnNames = {"user_id", "name"}))
@AllArgsConstructor
@NoArgsConstructor
@Generated
public class WatchGroup {

    public static final int NAME_MAX_LENGTH = 64;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = NAME_MAX_LENGTH)
    private String name;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "group", cascade = CascadeType.REMOVE)
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<TickerWatch> watches;
}
