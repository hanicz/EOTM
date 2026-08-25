package eye.on.the.money.repository.etf;

import eye.on.the.money.model.etf.ETFInvestment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ETFInvestmentRepository extends JpaRepository<ETFInvestment, Long> {
    Optional<ETFInvestment> findByIdAndUserEmail(Long id, String userEmail);

    List<ETFInvestment> findByUserEmailAndTransactionDateBetweenOrderByTransactionDate(
            String userEmail, LocalDate from, LocalDate to);

    List<ETFInvestment> findByUserEmailOrderByTransactionDate(String userEmail);

    List<ETFInvestment> findByUserEmailOrderByTransactionDateDesc(String userEmail);

    void deleteByUserEmailAndIdIn(String userEmail, List<Long> ids);
}
