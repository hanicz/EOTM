package eye.on.the.money.service.etf.impl;

import eye.on.the.money.model.etf.ETFPayment;
import eye.on.the.money.model.Currency;
import eye.on.the.money.repository.etf.ETFPaymentRepository;
import eye.on.the.money.service.etf.ETFPaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ETFPaymentServiceImpl implements ETFPaymentService {

    @Autowired
    private ETFPaymentRepository etfPaymentRepository;

    @Transactional
    @Override
    public ETFPayment createPayment(Currency currency, Double amount) {
        return this.etfPaymentRepository.save(ETFPayment.builder().amount(amount).currency(currency).build());
    }

    @Transactional
    @Override
    public ETFPayment updatePayment(ETFPayment payment) {
        return this.etfPaymentRepository.save(payment);
    }
}
