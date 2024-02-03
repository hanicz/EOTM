package eye.on.the.money.repository.stock;

import eye.on.the.money.model.stock.Stock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StockRepository extends JpaRepository<Stock, String> {
    Optional<Stock> findByName(String name);

    Optional<Stock> findByShortName(String shortName);

    List<Stock> findAllByOrderByShortNameAsc();
}