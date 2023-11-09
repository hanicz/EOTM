package eye.on.the.money.repository.crypto;

import eye.on.the.money.model.crypto.Coin;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface CoinRepository extends CrudRepository<Coin, String> {
    Optional<Coin> findByName(String name);
    Optional<Coin> findBySymbol(String symbol);
    List<Coin> findAllByOrderByNameAsc();
}
