package eye.on.the.money.repository;

import eye.on.the.money.model.stock.Dividend;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface DividendRepository extends CrudRepository<Dividend, Long> {
    public List<Dividend> findByUser_IdOrderByDividendDate(Long userId);
    public void deleteByUser_idAndIdIn(Long userId, List<Long> ids);
    public Optional<Dividend> findByIdAndUser_Id(Long id, Long userId);
}
