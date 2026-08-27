package eye.on.the.money.repository.security;

import eye.on.the.money.model.security.SecurityTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SecurityTransactionRepository extends JpaRepository<SecurityTransaction, Long> {
    List<SecurityTransaction> findByUserIdOrderByTransactionDateDesc(Long userId);

    List<SecurityTransaction> findByUserIdAndTransactionDateBetweenOrderByTransactionDate(
            Long userId, LocalDate from, LocalDate to);

    List<SecurityTransaction> findByUserIdOrderByTransactionDate(Long userId);

    void deleteByUserIdAndIdIn(Long userId, List<Long> ids);

    Optional<SecurityTransaction> findByIdAndUserId(Long id, Long userId);
}
