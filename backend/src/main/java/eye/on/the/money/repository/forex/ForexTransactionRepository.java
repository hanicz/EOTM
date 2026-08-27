package eye.on.the.money.repository.forex;

import eye.on.the.money.model.forex.ForexTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ForexTransactionRepository extends JpaRepository<ForexTransaction, Long> {
    List<ForexTransaction> findByUserIdOrderByTransactionDate(Long userId);

    List<ForexTransaction> findByUserIdAndTransactionDateBetweenOrderByTransactionDate(
            Long userId, LocalDate from, LocalDate to);

    void deleteByUserIdAndIdIn(Long userId, List<Long> ids);

    Optional<ForexTransaction> findByIdAndUserId(Long id, Long userId);
}
