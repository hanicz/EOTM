package eye.on.the.money.repository.report;

import eye.on.the.money.model.report.ReportSubscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReportSubscriptionRepository extends JpaRepository<ReportSubscription, Long> {
    Optional<ReportSubscription> findByUserEmail(String userEmail);

    List<ReportSubscription> findByEnabledTrue();
}
