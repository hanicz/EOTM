package eye.on.the.money.repository.stock;

import eye.on.the.money.model.stock.StockPayment;
import org.springframework.data.repository.CrudRepository;

public interface StockPaymentRepository extends CrudRepository<StockPayment, Long> {
}
