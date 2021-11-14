package eye.on.the.money.service.impl;

import eye.on.the.money.model.Currency;
import eye.on.the.money.model.stock.StockPayment;
import eye.on.the.money.repository.StockPaymentRepository;
import eye.on.the.money.service.StockPaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class StockPaymentServiceImpl implements StockPaymentService {

    @Autowired
    private StockPaymentRepository stockPaymentRepository;

    public StockPayment createNewPayment(Currency currency, Double amount){
        return this.stockPaymentRepository.save(StockPayment.builder().amount(amount).currency(currency).build());
    }
}
