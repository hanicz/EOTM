package eye.on.the.money.repository.crypto;

import eye.on.the.money.model.crypto.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByUserIdOrderByTransactionDate(Long userId);

    List<Transaction> findByUserIdAndTransactionDateBetweenOrderByTransactionDate(
            Long userId, LocalDate from, LocalDate to);
    List<Transaction> findByUserIdOrderByTransactionDateDesc(Long userId);

    int deleteByUserIdAndIdIn(Long userId, List<Long> ids);

    Optional<Transaction> findByIdAndUserId(Long id, Long userId);
}
