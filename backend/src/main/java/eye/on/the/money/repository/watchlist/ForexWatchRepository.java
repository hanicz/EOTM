package eye.on.the.money.repository.watchlist;

import eye.on.the.money.model.watchlist.ForexWatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ForexWatchRepository extends JpaRepository<ForexWatch, Long> {
    List<ForexWatch> findByUserIdOrderByFromCurrencyAscToCurrencyAsc(Long userId);

    Optional<ForexWatch> findByUserIdAndFromCurrencyIdAndToCurrencyId(Long userId, String fromCurrencyId,
                                                                        String toCurrencyId);

    void deleteByIdAndUserId(Long id, Long userId);
}
