package eye.on.the.money.repository.financial;

import eye.on.the.money.model.financial.AccountSide;
import eye.on.the.money.model.financial.BankExclusionRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BankExclusionRuleRepository extends JpaRepository<BankExclusionRule, Long> {

    List<BankExclusionRule> findByUserIdOrderByAccountNumberAsc(Long userId);

    List<BankExclusionRule> findByUserIdAndActiveTrue(Long userId);

    Optional<BankExclusionRule> findByIdAndUserId(Long id, Long userId);

    Optional<BankExclusionRule> findByUserIdAndNormalizedAccountAndSide(Long userId, String normalizedAccount,
                                                                       AccountSide side);

    void deleteByUserIdAndIdIn(Long userId, List<Long> ids);
}
