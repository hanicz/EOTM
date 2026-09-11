package eye.on.the.money.service.forex;

import eye.on.the.money.EotmApplication;
import eye.on.the.money.dto.out.CurrencyDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

@SpringBootTest(classes = EotmApplication.class)
@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
class CurrencyServiceTest {

    @Autowired
    private CurrencyService currencyService;

    @Test
    public void getCurrenciesSortedById() {
        List<CurrencyDTO> currencies = this.currencyService.getCurrencies();

        Assertions.assertEquals(List.of("EUR", "HUF", "USD"), currencies.stream().map(CurrencyDTO::getId).toList());
        Assertions.assertEquals("euro", currencies.getFirst().getName());
    }
}
