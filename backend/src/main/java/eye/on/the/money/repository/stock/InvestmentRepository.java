package eye.on.the.money.repository.stock;

import eye.on.the.money.model.stock.Investment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface InvestmentRepository extends JpaRepository<Investment, Long> {
    List<Investment> findByUserIdOrderByTransactionDateDesc(Long userId);

    List<Investment> findByUserIdAndTransactionDateBetweenOrderByTransactionDate(
            Long userId, LocalDate from, LocalDate to);
    List<Investment> findByUserIdOrderByTransactionDate(Long userId);
    List<Investment> findByUserIdAndAccountIdOrderByTransactionDateDesc(Long userId, Long accountId);

    List<Investment> findByUserIdAndRsuTrueOrderByTransactionDateDesc(Long userId);

    List<Investment> findByUserIdAndIdIn(Long userId, List<Long> ids);

    Optional<Investment> findByIdAndUserId(Long id, Long userId);

    void deleteByUserIdAndIdIn(Long userId, List<Long> ids);
}
