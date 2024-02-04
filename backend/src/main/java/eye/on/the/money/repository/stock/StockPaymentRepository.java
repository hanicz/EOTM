package eye.on.the.money.repository.stock;

import eye.on.the.money.model.stock.StockPayment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockPaymentRepository extends JpaRepository<StockPayment, Long> {
}
