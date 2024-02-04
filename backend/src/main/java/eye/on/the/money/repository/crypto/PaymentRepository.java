package eye.on.the.money.repository.crypto;

import eye.on.the.money.model.crypto.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
}
