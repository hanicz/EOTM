package eye.on.the.money.service.crypto;

import eye.on.the.money.model.Currency;
import eye.on.the.money.model.crypto.Payment;
import eye.on.the.money.repository.crypto.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;

    @Transactional
    public Payment createPayment(Currency currency, Double amount) {
        return this.paymentRepository.save(Payment.builder().amount(amount).currency(currency).build());
    }

    @Transactional
    public Payment updatePayment(Payment payment) {
        return this.paymentRepository.save(payment);
    }
}
