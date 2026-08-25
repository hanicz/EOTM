package eye.on.the.money.repository.forex;

import eye.on.the.money.model.forex.ForexTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ForexTransactionRepository extends JpaRepository<ForexTransaction, Long> {
    List<ForexTransaction> findByUserEmailOrderByTransactionDate(String userEmail);

    List<ForexTransaction> findByUserEmailAndTransactionDateBetweenOrderByTransactionDate(
            String userEmail, LocalDate from, LocalDate to);

    void deleteByUserEmailAndIdIn(String userEmail, List<Long> ids);

    Optional<ForexTransaction> findByIdAndUserEmail(Long id, String userEmail);
}
