package eye.on.the.money.model.report;

import com.fasterxml.jackson.annotation.JsonIgnore;
import eye.on.the.money.model.User;
import eye.on.the.money.util.Generated;
import jakarta.persistence.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Builder
@Slf4j
@ToString(exclude = "user")
@Table(name = "EOTM_REPORT_SUBSCRIPTION",
        uniqueConstraints = @UniqueConstraint(name = "UK_EOTM_REPORT_SUBSCRIPTION_USER", columnNames = "user_id"))
@AllArgsConstructor
@NoArgsConstructor
@Generated
@EqualsAndHashCode(exclude = "user")
public class ReportSubscription {

    public static final int MAX_RECIPIENTS = 5;
    public static final String DEFAULT_CURRENCY = "HUF";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @Column(nullable = false)
    private boolean enabled;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "last_sent_period", length = 7)
    private String lastSentPeriod;

    @Column(name = "last_manual_send_at")
    private LocalDateTime lastManualSendAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "EOTM_REPORT_RECIPIENT", joinColumns = @JoinColumn(name = "subscription_id"))
    @Column(name = "email", nullable = false, length = User.EMAIL_MAX_LENGTH)
    @Builder.Default
    private List<String> recipients = new ArrayList<>();
}
