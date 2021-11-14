package eye.on.the.money.service;

import eye.on.the.money.model.Currency;
import eye.on.the.money.model.stock.StockPayment;

public interface StockPaymentService {
    public StockPayment createNewPayment(Currency currency, Double amount);
}
