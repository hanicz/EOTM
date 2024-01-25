package eye.on.the.money.service.crypto;

import eye.on.the.money.EotmApplication;
import eye.on.the.money.model.Currency;
import eye.on.the.money.model.crypto.Payment;
import eye.on.the.money.repository.crypto.PaymentRepository;
import eye.on.the.money.repository.forex.CurrencyRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = EotmApplication.class)
@ActiveProfiles("test")
class PaymentServiceTest {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private CurrencyRepository currencyRepository;

    @Test
    public void createPayment() {
        Currency currency = this.currencyRepository.findByName("euro").get();
        Payment result = this.paymentService.createPayment(currency, 9994.2);

        Assertions.assertAll("Check payment attributes",
                () -> assertEquals(9994.2, result.getAmount()),
                () -> assertEquals(currency, result.getCurrency()));
    }

    @Test
    public void updatePayment() {
        Currency currency = this.currencyRepository.findByName("US dollar").get();
        Payment payment = Payment.builder().currency(currency).amount(9994.2).id(1L).build();

        Payment result = this.paymentService.updatePayment(payment);

        Assertions.assertAll("Check payment attributes",
                () -> assertEquals(payment.getAmount(), result.getAmount()),
                () -> assertEquals(payment.getCurrency(), result.getCurrency()));
    }
}