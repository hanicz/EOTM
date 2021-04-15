package eye.on.the.money.repository;

import eye.on.the.money.model.Currency;
import org.springframework.data.repository.CrudRepository;

public interface CurrencyRepository extends CrudRepository<Currency, String> {
}
