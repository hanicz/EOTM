package eye.on.the.money.repository;

import eye.on.the.money.model.crypto.Coin;
import org.springframework.data.repository.CrudRepository;

public interface CoinRepository extends CrudRepository<Coin, String> {
}
