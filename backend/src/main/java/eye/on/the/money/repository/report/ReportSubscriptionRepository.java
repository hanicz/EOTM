package eye.on.the.money.repository.report;

import eye.on.the.money.model.report.ReportSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReportSubscriptionRepository extends JpaRepository<ReportSubscription, Long> {
    Optional<ReportSubscription> findByUserId(Long userId);

    List<ReportSubscription> findByEnabledTrue();

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update ReportSubscription s set s.lastManualSendAt = :now "
            + "where s.id = :id and (s.lastManualSendAt is null or s.lastManualSendAt <= :claimableBefore)")
    int claimManualSend(@Param("id") Long id,
                        @Param("now") LocalDateTime now,
                        @Param("claimableBefore") LocalDateTime claimableBefore);
}
