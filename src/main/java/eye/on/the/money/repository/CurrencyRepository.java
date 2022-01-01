package eye.on.the.money.repository;

import eye.on.the.money.model.forex.Currency;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface CurrencyRepository extends CrudRepository<Currency, String> {
    public Optional<Currency> findByName(String name);
}
