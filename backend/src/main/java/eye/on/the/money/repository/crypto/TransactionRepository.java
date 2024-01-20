package eye.on.the.money.repository.crypto;

import eye.on.the.money.model.crypto.Transaction;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends CrudRepository<Transaction, Long> {
    List<Transaction> findByUserEmailOrderByTransactionDate(String userEmail);
    void deleteByUserEmailAndIdIn(String userEmail, List<Long> ids);
    Optional<Transaction> findByIdAndUserEmail(Long id, String userEmail);
}
