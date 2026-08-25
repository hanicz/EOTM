package eye.on.the.money.repository.security;

import eye.on.the.money.model.security.SecurityRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SecurityRateRepository extends JpaRepository<SecurityRate, Long> {

    Optional<SecurityRate> findByIsinAndPeriodStart(String isin, LocalDate periodStart);

    @Query("SELECT MAX(r.fetchedAt) FROM SecurityRate r")
    Optional<LocalDateTime> findLastFetchedAt();

    List<SecurityRate> findByIsinInAndPaymentDateGreaterThanEqualOrderByPaymentDateAsc(
            Collection<String> isins, LocalDate from);
}
