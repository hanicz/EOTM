package eye.on.the.money.repository;

import eye.on.the.money.model.Currency;
import eye.on.the.money.model.crypto.Transaction;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface CurrencyRepository extends CrudRepository<Currency, String> {
    public Optional<Currency> findByName(String name);
}
