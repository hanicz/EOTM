package eye.on.the.money.repository.etf;

import eye.on.the.money.model.etf.ETFPayment;
import org.springframework.data.repository.CrudRepository;

public interface ETFPaymentRepository extends CrudRepository<ETFPayment, Long> {
}
