package eye.on.the.money.service.impl;

import eye.on.the.money.model.Currency;
import eye.on.the.money.model.crypto.Payment;
import eye.on.the.money.repository.PaymentRepository;
import eye.on.the.money.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PaymentServiceImpl implements PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;

    @Override
    public Payment createPayment(Currency currency, Double amount) {
        return this.paymentRepository.save(Payment.builder().amount(amount).currency(currency).build());
    }

    @Override
    public Payment updatePayment(Payment payment) {
        return this.paymentRepository.save(payment);
    }
}
