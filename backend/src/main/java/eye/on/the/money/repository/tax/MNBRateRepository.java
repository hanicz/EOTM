package eye.on.the.money.repository.tax;

import eye.on.the.money.model.tax.MNBRate;
import org.springframework.data.repository.CrudRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface MNBRateRepository extends CrudRepository<MNBRate, Long> {
    Optional<MNBRate> findByCurrency_IdAndRateDate(String currencyId, LocalDate rateDate);
}
