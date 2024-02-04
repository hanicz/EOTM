package eye.on.the.money.repository.tax;

import eye.on.the.money.model.tax.MNBRate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface MNBRateRepository extends JpaRepository<MNBRate, Long> {
    Optional<MNBRate> findByCurrency_IdAndRateDate(String currencyId, LocalDate rateDate);
}
