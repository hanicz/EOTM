package eye.on.the.money.repository.stock;

import eye.on.the.money.model.stock.Stock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockRepository extends JpaRepository<Stock, String> {
    List<Stock> findAllByOrderByShortNameAsc();
}
