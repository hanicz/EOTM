package eye.on.the.money.repository;

import eye.on.the.money.model.stock.Investment;
import org.springframework.data.repository.CrudRepository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface InvestmentRepository extends CrudRepository<Investment, String> {
    public List<Investment> findByUser_IdOrderByTransactionDate(Long userId);
    public List<Investment> findByUser_IdAndBuySell(Long userId, String buySell);
    public List<Investment> findByUser_IdAndTransactionDateBetween(Long userId, Date transactionDateStart, Date transactionDateEnd);
    public List<Investment> findByUser_IdAndBuySellAndTransactionDateBetween(Long userId, String buySell, Date transactionDateStart, Date transactionDateEnd);
    public Optional<Investment> findByIdAndUser_Id(Long id, Long userId);
}
