package eye.on.the.money.service.stock;

import eye.on.the.money.EotmApplication;
import eye.on.the.money.model.Currency;
import eye.on.the.money.model.stock.StockPayment;
import eye.on.the.money.repository.forex.CurrencyRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = EotmApplication.class)
@ActiveProfiles("test")
class StockPaymentServiceTest {

    @Autowired
    private StockPaymentService stockPaymentService;

    @Autowired
    private CurrencyRepository currencyRepository;

    @Test
    public void createNewPayment() {
        Currency currency = this.currencyRepository.findByName("euro").get();
        StockPayment result = this.stockPaymentService.createNewPayment(currency, 667.6);

        Assertions.assertAll("Check payment attributes",
                () -> assertEquals(667.6, result.getAmount()),
                () -> assertEquals(currency, result.getCurrency()));
    }
}