package eye.on.the.money.repository.etf;

import eye.on.the.money.model.etf.ETFInvestment;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface ETFInvestmentRepository extends CrudRepository<ETFInvestment, Long> {
    public Optional<ETFInvestment> findByIdAndUser_Id(Long id, Long userId);

    public List<ETFInvestment> findByUser_IdOrderByTransactionDate(Long userId);

    public void deleteByUser_IdAndIdIn(Long userId, List<Long> ids);
}
