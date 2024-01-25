package eye.on.the.money.service.stock;

import eye.on.the.money.model.Currency;
import eye.on.the.money.model.stock.StockPayment;
import eye.on.the.money.repository.stock.StockPaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StockPaymentService {

    private final StockPaymentRepository stockPaymentRepository;

    @Transactional
    public StockPayment createNewPayment(Currency currency, Double amount) {
        return this.stockPaymentRepository.save(StockPayment.builder().amount(amount).currency(currency).build());
    }
}
