package eye.on.the.money.service.impl;

import eye.on.the.money.model.Currency;
import eye.on.the.money.model.Payment;
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
        Payment payment = this.paymentRepository.save(Payment.builder().amount(amount).currency(currency).build());
        return payment;
    }

    @Override
    public Payment updatePayment(Payment payment) {
        return this.paymentRepository.save(payment);
    }
}
