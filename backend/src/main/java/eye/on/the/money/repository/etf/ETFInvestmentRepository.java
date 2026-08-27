package eye.on.the.money.repository.etf;

import eye.on.the.money.model.etf.ETFInvestment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ETFInvestmentRepository extends JpaRepository<ETFInvestment, Long> {
    Optional<ETFInvestment> findByIdAndUserId(Long id, Long userId);

    List<ETFInvestment> findByUserIdAndTransactionDateBetweenOrderByTransactionDate(
            Long userId, LocalDate from, LocalDate to);

    List<ETFInvestment> findByUserIdOrderByTransactionDate(Long userId);

    List<ETFInvestment> findByUserIdOrderByTransactionDateDesc(Long userId);

    List<ETFInvestment> findByUserIdAndAccountIdOrderByTransactionDateDesc(Long userId, Long accountId);

    void deleteByUserIdAndIdIn(Long userId, List<Long> ids);
}
