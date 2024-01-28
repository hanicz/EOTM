package eye.on.the.money.repository.watchlist;

import eye.on.the.money.model.watchlist.ForexWatch;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface ForexWatchRepository extends CrudRepository<ForexWatch, Long> {
    List<ForexWatch> findByUserEmail(String userEmail);

    void deleteByIdAndUserEmail(Long id, String userEmail);
}
