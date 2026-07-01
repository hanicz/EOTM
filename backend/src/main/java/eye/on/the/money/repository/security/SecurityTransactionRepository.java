package eye.on.the.money.repository.security;

import eye.on.the.money.model.security.SecurityTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SecurityTransactionRepository extends JpaRepository<SecurityTransaction, Long> {
    List<SecurityTransaction> findByUserEmailOrderByTransactionDateDesc(String userEmail);

    List<SecurityTransaction> findByUserEmailOrderByTransactionDate(String userEmail);

    void deleteByUserEmailAndIdIn(String userEmail, List<Long> ids);

    Optional<SecurityTransaction> findByIdAndUserEmail(Long id, String userEmail);
}
