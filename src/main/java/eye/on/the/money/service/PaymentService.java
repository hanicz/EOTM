package eye.on.the.money.service;

import eye.on.the.money.model.forex.Currency;
import eye.on.the.money.model.crypto.Payment;

public interface PaymentService {
    public Payment createPayment(Currency currency, Double amount);

    public Payment updatePayment(Payment payment);
}
