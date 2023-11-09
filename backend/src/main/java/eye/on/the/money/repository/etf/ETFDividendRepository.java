package eye.on.the.money.repository.etf;

import eye.on.the.money.model.etf.ETFDividend;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface ETFDividendRepository extends CrudRepository<ETFDividend, Long> {
    List<ETFDividend> findByUser_IdOrderByDividendDate(Long userId);
    void deleteByUser_idAndIdIn(Long userId, List<Long> ids);
    Optional<ETFDividend> findByIdAndUser_Id(Long id, Long userId);
}
