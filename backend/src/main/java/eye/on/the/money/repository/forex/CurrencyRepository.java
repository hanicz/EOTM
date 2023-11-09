package eye.on.the.money.repository.forex;

import eye.on.the.money.model.Currency;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface CurrencyRepository extends CrudRepository<Currency, String> {
    Optional<Currency> findByName(String name);
}
