package eye.on.the.money.repository.watchlist;

import eye.on.the.money.model.watchlist.TickerWatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockWatchRepository extends JpaRepository<TickerWatch, Long> {
    List<TickerWatch> findByUserEmailOrderByStockShortName(String userEmail);

    void deleteByIdAndUserEmail(Long id, String userEmail);
}
