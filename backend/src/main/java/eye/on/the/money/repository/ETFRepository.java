package eye.on.the.money.repository;

import eye.on.the.money.model.etf.ETF;
import eye.on.the.money.model.stock.Stock;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface ETFRepository extends CrudRepository<ETF, String> {
    public Optional<Stock> findByName(String name);

    public Optional<Stock> findByShortName(String shortName);

    public List<Stock> findAllByOrderByShortNameAsc();
}
