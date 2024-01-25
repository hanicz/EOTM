package eye.on.the.money.repository.stock;

import eye.on.the.money.model.stock.Investment;
import org.springframework.data.repository.CrudRepository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface InvestmentRepository extends CrudRepository<Investment, Long> {
    List<Investment> findByUserEmailOrderByTransactionDate(String userEmail);

    List<Investment> findByUserEmailAndBuySellAndTransactionDateBetween(String userEmail, String buySell, Date transactionDateStart, Date transactionDateEnd);

    Optional<Investment> findByIdAndUserEmail(Long id, String userEmail);

    void deleteByUserEmailAndIdIn(String userEmail, List<Long> ids);
}
