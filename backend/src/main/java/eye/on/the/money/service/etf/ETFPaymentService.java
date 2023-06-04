package eye.on.the.money.service.etf;

import eye.on.the.money.model.etf.ETFPayment;
import eye.on.the.money.model.Currency;

public interface ETFPaymentService {
    public ETFPayment createPayment(Currency currency, Double amount);
    public ETFPayment updatePayment(ETFPayment payment);
}
