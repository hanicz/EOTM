package eye.on.the.money.repository;

import eye.on.the.money.model.watchlist.ForexWatch;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface ForexWatchRepository extends CrudRepository<ForexWatch, Long> {
    public List<ForexWatch> findByUser_Id(Long userId);
    public void deleteByIdAndUser_Id(Long id, Long userId);
}
