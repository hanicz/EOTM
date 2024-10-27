package eye.on.the.money.service.etf;

import eye.on.the.money.EotmApplication;
import eye.on.the.money.model.Currency;
import eye.on.the.money.model.etf.ETFPayment;
import eye.on.the.money.repository.forex.CurrencyRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = EotmApplication.class)
@ActiveProfiles("test")
class ETFPaymentServiceTest {

    @Autowired
    private CurrencyRepository currencyRepository;

    @Autowired
    private ETFPaymentService etfPaymentService;

    @Test
    public void createPayment() {
        Currency currency = this.currencyRepository.findByName("US dollar").get();
        ETFPayment result = this.etfPaymentService.createPayment(currency, 701.3);

        Assertions.assertAll("Check payment attributes",
                () -> assertEquals(701.3, result.getAmount()),
                () -> assertEquals(currency, result.getCurrency()));
    }

}