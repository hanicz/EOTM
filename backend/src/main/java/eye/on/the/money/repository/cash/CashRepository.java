package eye.on.the.money.repository.cash;

import eye.on.the.money.model.cash.Cash;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CashRepository extends JpaRepository<Cash, Long> {

    Optional<Cash> findByUserId(Long userId);
}
