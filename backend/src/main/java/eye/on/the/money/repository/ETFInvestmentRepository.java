package eye.on.the.money.repository;

import eye.on.the.money.model.etf.ETFInvestment;
import eye.on.the.money.model.stock.Investment;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface ETFInvestmentRepository extends CrudRepository<ETFInvestment, Long> {
    public Optional<Investment> findByIdAndUser_Id(Long id, Long userId);

    public void deleteByUser_IdAndIdIn(Long userId, List<Long> ids);
}
