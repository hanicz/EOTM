package eye.on.the.money.repository.stock;

import eye.on.the.money.model.stock.Stock;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface StockRepository extends CrudRepository<Stock, String> {
    public Optional<Stock> findByName(String name);
    public Optional<Stock> findByShortName(String shortName);
    public List<Stock> findAllByOrderByShortNameAsc();
}