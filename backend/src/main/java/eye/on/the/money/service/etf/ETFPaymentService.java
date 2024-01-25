package eye.on.the.money.service.etf;

import eye.on.the.money.model.Currency;
import eye.on.the.money.model.etf.ETFPayment;
import eye.on.the.money.repository.etf.ETFPaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ETFPaymentService {

    private final ETFPaymentRepository etfPaymentRepository;

    @Autowired
    public ETFPaymentService(ETFPaymentRepository etfPaymentRepository) {
        this.etfPaymentRepository = etfPaymentRepository;
    }

    @Transactional
    public ETFPayment createPayment(Currency currency, Double amount) {
        return this.etfPaymentRepository.save(ETFPayment.builder().amount(amount).currency(currency).build());
    }

    @Transactional
    public ETFPayment updatePayment(ETFPayment payment) {
        return this.etfPaymentRepository.save(payment);
    }
}
