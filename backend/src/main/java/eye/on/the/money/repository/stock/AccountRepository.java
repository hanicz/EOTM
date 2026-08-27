package eye.on.the.money.repository.stock;

import eye.on.the.money.model.stock.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {
    Optional<Account> findByUserIdAndId(Long userId, Long id);

    List<Account> findByUserIdOrderByAccountName(Long userId);

    int deleteByUserIdAndId(Long userId, Long id);
}
