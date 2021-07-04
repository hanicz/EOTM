package eye.on.the.money.repository;

import eye.on.the.money.model.crypto.Payment;
import org.springframework.data.repository.CrudRepository;

public interface PaymentRepository extends CrudRepository<Payment, Long> {
}
