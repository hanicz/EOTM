package eye.on.the.money.repository.salary;

import eye.on.the.money.model.salary.CompensationItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CompensationRepository extends JpaRepository<CompensationItem, Long> {

    List<CompensationItem> findByUserIdOrderByNameAscIdAsc(Long userId);

    Optional<CompensationItem> findByIdAndUserId(Long id, Long userId);

    void deleteByUserIdAndIdIn(Long userId, List<Long> ids);
}
