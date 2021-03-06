package eye.on.the.money.repository.tax;

import eye.on.the.money.model.tax.MNBRate;
import org.springframework.data.repository.CrudRepository;

import java.util.Date;
import java.util.Optional;

public interface MNBRateRepository extends CrudRepository<MNBRate, Long> {
    public Optional<MNBRate> findByCurrency_IdAndRateDate(String currencyId, Date rateDate);
}
