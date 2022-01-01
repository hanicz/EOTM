package eye.on.the.money.service.impl;

import eye.on.the.money.model.forex.Currency;
import eye.on.the.money.model.crypto.Payment;
import eye.on.the.money.repository.PaymentRepository;
import eye.on.the.money.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentServiceImpl implements PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;

    @Transactional
    @Override
    public Payment createPayment(Currency currency, Double amount) {
        return this.paymentRepository.save(Payment.builder().amount(amount).currency(currency).build());
    }

    @Transactional
    @Override
    public Payment updatePayment(Payment payment) {
        return this.paymentRepository.save(payment);
    }
}
