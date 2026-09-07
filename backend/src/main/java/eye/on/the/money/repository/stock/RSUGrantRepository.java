package eye.on.the.money.repository.stock;

import eye.on.the.money.model.stock.RSUGrant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RSUGrantRepository extends JpaRepository<RSUGrant, Long> {

    List<RSUGrant> findByUserIdOrderByGrantDateDescIdAsc(Long userId);

    Optional<RSUGrant> findByIdAndUserId(Long id, Long userId);

    void deleteByUserIdAndIdIn(Long userId, List<Long> ids);
}
