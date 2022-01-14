package eye.on.the.money.repository.forex;

import eye.on.the.money.model.forex.ForexTransaction;
import org.springframework.data.repository.CrudRepository;

public interface ForexTransactionRepository extends CrudRepository<ForexTransaction, Long> {

}
