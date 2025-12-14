package eye.on.the.money.controller;

import eye.on.the.money.model.crypto.Coin;
import eye.on.the.money.service.crypto.CoinService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class CoinControllerTest {

    @Mock
    private CoinService coinService;

    @InjectMocks
    private CoinController coinController;

    @Test
    public void getAllCoins() {
        List<Coin> coins = new ArrayList<>();
        coins.add(Coin.builder().id("btc").name("bitcoin").symbol("btc").build());
        coins.add(Coin.builder().id("eth").name("ethereum").symbol("eth").build());
        coins.add(Coin.builder().id("dot").name("polkadot").symbol("dot").build());
        when(this.coinService.getAllCoins()).thenReturn(coins);

        Assertions.assertIterableEquals(coins, this.coinController.getAllCoins().getBody());
    }
}