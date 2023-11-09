package eye.on.the.money.repository.watchlist;

import eye.on.the.money.model.watchlist.CryptoWatch;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface CryptoWatchRepository extends CrudRepository<CryptoWatch, Long> {
    List<CryptoWatch> findByUser_IdOrderByCoin_Symbol(Long userId);
    void deleteByIdAndUser_Id(Long id, Long userId);
}
