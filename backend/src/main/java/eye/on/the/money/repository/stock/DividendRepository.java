package eye.on.the.money.repository.stock;

import eye.on.the.money.model.stock.Dividend;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface DividendRepository extends CrudRepository<Dividend, Long> {
    List<Dividend> findByUser_IdOrderByDividendDate(Long userId);
    void deleteByUser_idAndIdIn(Long userId, List<Long> ids);
    Optional<Dividend> findByIdAndUser_Id(Long id, Long userId);
}
