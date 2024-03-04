package eye.on.the.money.repository.stock;

import eye.on.the.money.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {
    Optional<Account> findByUserEmailAndId(String email, Long id);

    List<Account> findByUserEmailOrderByAccountName(String email);
}
