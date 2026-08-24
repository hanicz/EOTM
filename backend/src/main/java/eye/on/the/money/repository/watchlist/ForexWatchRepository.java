package eye.on.the.money.repository.watchlist;

import eye.on.the.money.model.watchlist.ForexWatch;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface ForexWatchRepository extends CrudRepository<ForexWatch, Long> {
    List<ForexWatch> findByUserEmailOrderByFromCurrencyAscToCurrencyAsc(String userEmail);

    Optional<ForexWatch> findByUserEmailAndFromCurrencyIdAndToCurrencyId(String userEmail, String fromCurrencyId,
                                                                        String toCurrencyId);

    void deleteByIdAndUserEmail(Long id, String userEmail);
}
