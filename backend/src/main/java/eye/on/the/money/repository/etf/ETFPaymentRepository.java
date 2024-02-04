package eye.on.the.money.repository.etf;

import eye.on.the.money.model.etf.ETFPayment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ETFPaymentRepository extends JpaRepository<ETFPayment, Long> {
}
