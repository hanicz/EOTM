package eye.on.the.money.repository.stock;

import eye.on.the.money.model.stock.Investment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface InvestmentRepository extends JpaRepository<Investment, Long> {
    List<Investment> findByUserEmailOrderByTransactionDateDesc(String userEmail);

    List<Investment> findByUserEmailAndTransactionDateBetweenOrderByTransactionDate(
            String userEmail, LocalDate from, LocalDate to);
    List<Investment> findByUserEmailOrderByTransactionDate(String userEmail);
    List<Investment> findByUserEmailAndAccountIdOrderByTransactionDateDesc(String userEmail, Long accountId);

    List<Investment> findByUserEmailAndRsuTrueOrderByTransactionDateDesc(String userEmail);

    List<Investment> findByUserEmailAndIdIn(String userEmail, List<Long> ids);

    Optional<Investment> findByIdAndUserEmail(Long id, String userEmail);

    void deleteByUserEmailAndIdIn(String userEmail, List<Long> ids);
}
