package eye.on.the.money.repository;

import eye.on.the.money.model.crypto.Coin;
import eye.on.the.money.model.crypto.Transaction;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface CoinRepository extends CrudRepository<Coin, String> {
    public Optional<Coin> findByName(String name);
}
