package eye.on.the.money.model.networth;

import com.fasterxml.jackson.annotation.JsonIgnore;
import eye.on.the.money.model.AuditedEntity;
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
@Table(name = "EOTM_NET_WORTH_SNAPSHOT",
        uniqueConstraints = @UniqueConstraint(name = "UK_EOTM_NET_WORTH_SNAPSHOT_USER_DATE_CLASS",
                columnNames = {"user_id", "snapshot_date", "asset_class"}))
@AllArgsConstructor
@NoArgsConstructor
@Generated
public class NetWorthSnapshot extends AuditedEntity {

    public static final int ASSET_CLASS_MAX_LENGTH = 20;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    @Column(name = "asset_class", nullable = false, length = ASSET_CLASS_MAX_LENGTH)
    private String assetClass;

    @Column(nullable = false)
    private Double spent;

    @Column(nullable = false)
    private Double worth;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;
}
