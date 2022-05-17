package eye.on.the.money.repository.etf;

import eye.on.the.money.model.etf.ETFDividend;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface ETFDividendRepository extends CrudRepository<ETFDividend, Long> {
    public List<ETFDividend> findByUser_IdOrderByDividendDate(Long userId);

    public void deleteByUser_idAndIdIn(Long userId, List<Long> ids);

    public Optional<ETFDividend> findByIdAndUser_Id(Long id, Long userId);
}
