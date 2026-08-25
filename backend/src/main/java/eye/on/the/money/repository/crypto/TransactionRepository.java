package eye.on.the.money.repository.crypto;

import eye.on.the.money.model.crypto.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByUserEmailOrderByTransactionDate(String userEmail);

    List<Transaction> findByUserEmailAndTransactionDateBetweenOrderByTransactionDate(
            String userEmail, LocalDate from, LocalDate to);
    List<Transaction> findByUserEmailOrderByTransactionDateDesc(String userEmail);

    int deleteByUserEmailAndIdIn(String userEmail, List<Long> ids);

    Optional<Transaction> findByIdAndUserEmail(Long id, String userEmail);
}
