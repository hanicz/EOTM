package eye.on.the.money.repository.etf;

import eye.on.the.money.model.etf.ETFInvestment;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface ETFInvestmentRepository extends CrudRepository<ETFInvestment, Long> {
    Optional<ETFInvestment> findByIdAndUser_Id(Long id, Long userId);
    List<ETFInvestment> findByUser_IdOrderByTransactionDate(Long userId);
    void deleteByUser_IdAndIdIn(Long userId, List<Long> ids);
}
