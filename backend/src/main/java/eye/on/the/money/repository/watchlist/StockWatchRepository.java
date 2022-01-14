package eye.on.the.money.repository.watchlist;

import eye.on.the.money.model.watchlist.TickerWatch;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface StockWatchRepository extends CrudRepository<TickerWatch, Long> {
    public List<TickerWatch> findByUser_IdOrderByStockShortName(Long userId);
    public void deleteByIdAndUser_Id(Long id, Long userId);
}
