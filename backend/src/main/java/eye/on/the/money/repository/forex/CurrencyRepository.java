package eye.on.the.money.repository.forex;

import eye.on.the.money.model.Currency;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CurrencyRepository extends JpaRepository<Currency, String> {
    Optional<Currency> findByName(String name);
}
