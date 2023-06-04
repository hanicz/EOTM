package eye.on.the.money.repository.forex;

import eye.on.the.money.model.forex.ForexTransaction;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface ForexTransactionRepository extends CrudRepository<ForexTransaction, Long> {
    public List<ForexTransaction> findByUser_IdOrderByTransactionDate(Long userId);
    public void deleteByUser_IdAndIdIn(Long userId, List<Long> ids);
    public Optional<ForexTransaction> findByIdAndUser_Id(Long id, Long userId);
}
