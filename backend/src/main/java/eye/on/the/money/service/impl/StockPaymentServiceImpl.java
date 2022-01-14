package eye.on.the.money.service.impl;

import eye.on.the.money.model.forex.Currency;
import eye.on.the.money.model.stock.StockPayment;
import eye.on.the.money.repository.stock.StockPaymentRepository;
import eye.on.the.money.service.StockPaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StockPaymentServiceImpl implements StockPaymentService {

    @Autowired
    private StockPaymentRepository stockPaymentRepository;

    @Transactional
    @Override
    public StockPayment createNewPayment(Currency currency, Double amount){
        return this.stockPaymentRepository.save(StockPayment.builder().amount(amount).currency(currency).build());
    }
}
