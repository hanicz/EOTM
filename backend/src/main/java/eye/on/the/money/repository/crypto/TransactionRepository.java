package eye.on.the.money.repository.crypto;

import eye.on.the.money.model.crypto.Transaction;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends CrudRepository<Transaction, Long> {
    public List<Transaction> findByUser_IdOrderByTransactionDate(Long userId);
    public void deleteByUser_IdAndIdIn(Long userId, List<Long> ids);
    public Optional<Transaction> findByIdAndUser_Id(Long id, Long userId);
}
