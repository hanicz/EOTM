package eye.on.the.money.service;

import eye.on.the.money.model.Currency;
import eye.on.the.money.model.Payment;

public interface PaymentService {
    public Payment createPayment(Currency currency, Double amount);

    public Payment updatePayment(Payment payment);
}
