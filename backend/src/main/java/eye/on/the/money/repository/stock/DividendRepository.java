package eye.on.the.money.repository.stock;

import eye.on.the.money.model.stock.Dividend;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface DividendRepository extends CrudRepository<Dividend, Long> {
    List<Dividend> findByUserEmailOrderByDividendDate(String userEmail);
    void deleteByUserEmailAndIdIn(String userEmail, List<Long> ids);
    Optional<Dividend> findByIdAndUserEmail(Long id, String userEmail);
}
