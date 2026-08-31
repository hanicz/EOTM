package eye.on.the.money.repository.watchlist;

import eye.on.the.money.model.watchlist.WatchGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WatchGroupRepository extends JpaRepository<WatchGroup, Long> {

    List<WatchGroup> findByUserIdOrderByName(Long userId);

    Optional<WatchGroup> findByUserIdAndId(Long userId, Long id);

    Optional<WatchGroup> findByUserIdAndName(Long userId, String name);
}
