package eye.on.the.money.repository.stock;

import eye.on.the.money.model.stock.Investment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface InvestmentRepository extends JpaRepository<Investment, Long> {
    List<Investment> findByUserEmailOrderByTransactionDateDesc(String userEmail);
    List<Investment> findByUserEmailOrderByTransactionDate(String userEmail);
    List<Investment> findByUserEmailAndAccountIdOrderByTransactionDateDesc(String userEmail, Long accountId);

    List<Investment> findByUserEmailAndBuySellAndTransactionDateBetween(String userEmail, String buySell, Date transactionDateStart, Date transactionDateEnd);

    Optional<Investment> findByIdAndUserEmail(Long id, String userEmail);

    void deleteByUserEmailAndIdIn(String userEmail, List<Long> ids);
}
