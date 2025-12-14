package eye.on.the.money.service.shared;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eye.on.the.money.dto.out.CryptoWatchDTO;
import eye.on.the.money.model.crypto.Coin;
import eye.on.the.money.model.watchlist.CryptoWatch;
import eye.on.the.money.repository.crypto.CoinRepository;
import eye.on.the.money.repository.forex.CurrencyRepository;
import eye.on.the.money.repository.watchlist.CryptoWatchRepository;
import eye.on.the.money.repository.watchlist.ForexWatchRepository;
import eye.on.the.money.repository.watchlist.StockWatchRepository;
import eye.on.the.money.service.api.CryptoAPIService;
import eye.on.the.money.service.api.EODAPIService;
import eye.on.the.money.service.stock.StockService;
import eye.on.the.money.service.user.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class WatchListServiceTest {

    @Mock
    private CryptoWatchRepository cryptoWatchRepository;
    @Mock
    private StockWatchRepository stockWatchRepository;
    @Mock
    private ForexWatchRepository forexWatchRepository;
    @Mock
    private CurrencyRepository currencyRepository;
    @Mock
    private CryptoAPIService cryptoAPIService;
    @Mock
    private UserServiceImpl userService;
    @Mock
    private EODAPIService eodAPIService;
    @Mock
    private CoinRepository coinRepository;

    private final ModelMapper modelMapper = new ModelMapper();
    @Mock
    private StockService stockService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private WatchListService watchListService;

    @BeforeEach
    public void setUp() {
        this.watchListService = new WatchListService(
                this.cryptoWatchRepository,
                this.stockWatchRepository,
                this.forexWatchRepository,
                this.currencyRepository,
                this.cryptoAPIService,
                this.userService,
                this.eodAPIService,
                this.coinRepository,
                this.modelMapper,
                this.stockService
        );
    }

    @Test
    public void testGetCryptoWatchlistByUserId() throws JsonProcessingException {
        when(cryptoWatchRepository.findByUserEmailOrderByCoin_Symbol("test@example.com")).thenReturn(List.of(
                CryptoWatch.builder().id(1L).coin(Coin.builder().id("bitcoin").build()).build(),
                CryptoWatch.builder().id(2L).coin(Coin.builder().id("cardano").build()).build(),
                CryptoWatch.builder().id(3L).coin(Coin.builder().id("ethereum").build()).build()
        ));
        when(this.cryptoAPIService.getLiveValueForCoins("EUR", "bitcoin,cardano,ethereum")).thenReturn(this.objectMapper.readTree(
                """
                             {
                                 "bitcoin": {
                                     "eur": 44848,
                                     "eur_24h_change": 2.502835261063895
                                 },
                                 "cardano": {
                                     "eur": 0.512461,
                                     "eur_24h_change": 3.637176879372967
                                 },
                                 "ethereum": {
                                     "eur": 2345.27,
                                     "eur_24h_change": 1.7125094576440063
                                 }
                             }
                        """));

        List<CryptoWatchDTO> resultList = this.watchListService.getCryptoWatchlistByUserId("test@example.com", "EUR");

        assertEquals(3, resultList.size());
        assertEquals(44848.0, resultList.get(0).getLiveValue());
        assertEquals(2.502835261063895, resultList.get(0).getChange());
        assertEquals(0.512461, resultList.get(1).getLiveValue());
        assertEquals(3.637176879372967, resultList.get(1).getChange());
        assertEquals(2345.27, resultList.get(2).getLiveValue());
        assertEquals(1.7125094576440063, resultList.get(2).getChange());
    }

}