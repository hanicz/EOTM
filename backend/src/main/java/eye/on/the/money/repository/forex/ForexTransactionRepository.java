package eye.on.the.money.repository.forex;

import eye.on.the.money.model.forex.ForexTransaction;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface ForexTransactionRepository extends CrudRepository<ForexTransaction, Long> {
    List<ForexTransaction> findByUser_IdOrderByTransactionDate(Long userId);
    void deleteByUser_IdAndIdIn(Long userId, List<Long> ids);
    Optional<ForexTransaction> findByIdAndUser_Id(Long id, Long userId);
}
