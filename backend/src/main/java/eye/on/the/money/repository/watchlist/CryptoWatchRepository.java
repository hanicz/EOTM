package eye.on.the.money.repository.watchlist;

import eye.on.the.money.model.watchlist.CryptoWatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CryptoWatchRepository extends JpaRepository<CryptoWatch, Long> {
    List<CryptoWatch> findByUserIdOrderByCoin_Symbol(Long userId);

    Optional<CryptoWatch> findByUserIdAndCoinId(Long userId, String coinId);

    void deleteByIdAndUserId(Long id, Long userId);
}
