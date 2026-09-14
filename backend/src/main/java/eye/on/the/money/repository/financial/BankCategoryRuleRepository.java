package eye.on.the.money.repository.financial;

import eye.on.the.money.model.financial.BankCategoryRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BankCategoryRuleRepository extends JpaRepository<BankCategoryRule, Long> {

    List<BankCategoryRule> findByUserIdOrderByPriorityAscPatternAsc(Long userId);

    List<BankCategoryRule> findByUserIdAndActiveTrueOrderByPriorityAscPatternAsc(Long userId);

    Optional<BankCategoryRule> findByIdAndUserId(Long id, Long userId);

    Optional<BankCategoryRule> findByUserIdAndNormalizedPattern(Long userId, String normalizedPattern);

    void deleteByUserIdAndIdIn(Long userId, List<Long> ids);

    void deleteByUserIdAndCategoryIdIn(Long userId, List<Long> categoryIds);
}
