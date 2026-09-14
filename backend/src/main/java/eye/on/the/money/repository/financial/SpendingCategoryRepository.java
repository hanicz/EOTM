package eye.on.the.money.repository.financial;

import eye.on.the.money.model.financial.SpendingCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SpendingCategoryRepository extends JpaRepository<SpendingCategory, Long> {

    List<SpendingCategory> findByUserIdOrderByPositionAscNameAsc(Long userId);

    Optional<SpendingCategory> findByIdAndUserId(Long id, Long userId);

    Optional<SpendingCategory> findByUserIdAndNormalizedName(Long userId, String normalizedName);

    long countByUserId(Long userId);

    void deleteByUserIdAndIdIn(Long userId, List<Long> ids);
}
