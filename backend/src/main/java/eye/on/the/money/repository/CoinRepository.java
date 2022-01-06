package eye.on.the.money.repository;

import eye.on.the.money.model.crypto.Coin;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface CoinRepository extends CrudRepository<Coin, String> {
    public Optional<Coin> findByName(String name);

    public Optional<Coin> findBySymbol(String symbol);

    public List<Coin> findAllByOrderByNameAsc();
}
