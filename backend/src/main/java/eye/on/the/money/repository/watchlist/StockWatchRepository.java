package eye.on.the.money.repository.watchlist;

import eye.on.the.money.model.watchlist.TickerWatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StockWatchRepository extends JpaRepository<TickerWatch, Long> {
    List<TickerWatch> findByUserIdOrderByStockShortName(Long userId);

    Optional<TickerWatch> findByUserIdAndStockId(Long userId, String stockId);

    void deleteByIdAndUserId(Long id, Long userId);
}
