package eye.on.the.money.repository;

import eye.on.the.money.model.stock.Dividend;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface DividendRepository extends CrudRepository<Dividend, Long> {
    public List<Dividend> findByUser_IdOrderByDividendDate(Long userId);
    public void deleteByIdIn(List<Long> ids);
}
