package eye.on.the.money.service.etf;

import eye.on.the.money.EotmApplication;
import eye.on.the.money.model.etf.ETF;
import eye.on.the.money.repository.etf.ETFRepository;
import eye.on.the.money.service.api.EODAPIService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Optional;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = EotmApplication.class)
@ActiveProfiles("test")
class ETFServiceTest {

    @MockitoBean
    private EODAPIService eodAPIService;

    @Autowired
    private ETFRepository etfRepository;

    @Autowired
    private ETFService etfService;

    @Test
    public void getOrCreateETFExist() {
        ETF expected = this.etfRepository.findById("vwce.mi").get();
        ETF result = this.etfService.getOrCreateETF("vwce", "MI", "Vang FTSE AllW-A");

        Assertions.assertEquals(expected, result);
    }

    @Test
    public void getOrCreateETFNew() {
        Optional<ETF> empty = this.etfRepository.findById("newetf.us");
        ETF result = this.etfService.getOrCreateETF("newetf", "US", "New ETF Test");

        Assertions.assertAll(
                () -> Assertions.assertTrue(empty.isEmpty()),
                () -> Assertions.assertEquals(this.etfRepository.findById("newetf.us").get(), result));
    }

    @Test
    public void getOrCreateETFSeparatesSameTickerOnDifferentExchanges() {
        ETF milan = this.etfService.getOrCreateETF("VWCE", "MI", "Vang FTSE AllW-A");
        ETF xetra = this.etfService.getOrCreateETF("vwce", "xetra", "Vanguard FTSE All-World");

        Assertions.assertAll(
                () -> Assertions.assertEquals("vwce.mi", milan.getId()),
                () -> Assertions.assertEquals("vwce.xetra", xetra.getId()),
                () -> Assertions.assertEquals("MI", milan.getExchange()),
                () -> Assertions.assertEquals("XETRA", xetra.getExchange()),
                () -> Assertions.assertEquals(2, this.etfRepository.findAll().stream()
                        .filter(e -> "VWCE".equals(e.getShortName())).count()));
    }

    @Test
    public void getOrCreateETFRejectsBlankExchange() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> this.etfService.getOrCreateETF("VWCE", "", "Vanguard FTSE All-World"));
    }
}
