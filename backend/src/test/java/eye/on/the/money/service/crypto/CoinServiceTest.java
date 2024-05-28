package eye.on.the.money.service.crypto;

import eye.on.the.money.EotmApplication;
import eye.on.the.money.model.crypto.Coin;
import eye.on.the.money.repository.crypto.CoinRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;


@SpringBootTest(classes = EotmApplication.class)
@ActiveProfiles("test")
class CoinServiceTest {

    @Autowired
    private CoinRepository coinRepository;

    @Autowired
    private CoinService coinService;

    @Test
    public void getAllCoins() {
        List<Coin> expected = this.coinRepository.findAllByOrderByNameAsc();
        List<Coin> result = this.coinService.getAllCoins();

        Assertions.assertEquals(expected, result);
    }

    @Test
    public void getCoinBySymbol() {
        Coin expected = this.coinRepository.findBySymbol("BTC").get();
        Coin result = this.coinService.getCoinBySymbol("BTC");

        Assertions.assertEquals(expected, result);
    }
}