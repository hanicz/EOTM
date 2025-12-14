package eye.on.the.money.controller;

import eye.on.the.money.dto.out.CryptoWatchDTO;
import eye.on.the.money.dto.out.ForexWatchDTO;
import eye.on.the.money.dto.out.StockWatchDTO;
import eye.on.the.money.model.User;
import eye.on.the.money.model.stock.Stock;
import eye.on.the.money.service.shared.WatchListService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class WatchlistControllerTest {

    @Mock
    private WatchListService watchlistService;

    @InjectMocks
    private WatchlistController watchlistController;

    private final User user = User.builder().id(1L).email("email").build();

    @Test
    public void getCryptoWatchList() {
        List<CryptoWatchDTO> cDTO = new ArrayList<>();
        cDTO.add(CryptoWatchDTO.builder().cryptoWatchId(1L).liveValue(32.1).change(33.1).coinId("c1").name("n1").symbol("s1").build());
        cDTO.add(CryptoWatchDTO.builder().cryptoWatchId(2L).liveValue(632.1).change(333.1).coinId("c2").name("n2").symbol("s2").build());
        cDTO.add(CryptoWatchDTO.builder().cryptoWatchId(3L).liveValue(32.3).change(11.1).coinId("c3").name("n3").symbol("s3").build());

        when(this.watchlistService.getCryptoWatchlistByUserId(this.user.getEmail(), "eur")).thenReturn(cDTO);

        Assertions.assertIterableEquals(cDTO, this.watchlistController.getCryptoWatchList(this.user, "eur").getBody());
    }

    @Test
    public void getForexWatchList() {
        List<ForexWatchDTO> fDTO = new ArrayList<>();
        fDTO.add(ForexWatchDTO.builder().forexWatchID(1L).liveValue(32.1).change(33.1).pChange(3.1).fromCurrencyId("fc1").toCurrencyId("tc1").build());
        fDTO.add(ForexWatchDTO.builder().forexWatchID(2L).liveValue(632.1).change(333.1).pChange(5.1).fromCurrencyId("fc2").toCurrencyId("tc2").build());
        fDTO.add(ForexWatchDTO.builder().forexWatchID(3L).liveValue(32.3).change(11.1).pChange(3.3).fromCurrencyId("fc3").toCurrencyId("tc3").build());

        when(this.watchlistService.getForexWatchlistByUserId(this.user.getUsername())).thenReturn(fDTO);

        Assertions.assertIterableEquals(fDTO, this.watchlistController.getForexWatchList(this.user).getBody());
    }

    @Test
    public void getStockWatchList() {
        List<StockWatchDTO> sDTO = new ArrayList<>();
        sDTO.add(StockWatchDTO.builder().tickerWatchId(1L).liveValue(32.1).change(33.1).stockExchange("e1").stockName("n1").stockShortName("s1").pChange(3.1).build());
        sDTO.add(StockWatchDTO.builder().tickerWatchId(2L).liveValue(632.1).change(333.1).stockExchange("e2").stockName("n2").stockShortName("s2").pChange(1.1).build());
        sDTO.add(StockWatchDTO.builder().tickerWatchId(3L).liveValue(32.3).change(11.1).stockExchange("e3").stockName("n3").stockShortName("s3").pChange(3.3).build());

        when(this.watchlistService.getStockWatchlistByUserId(this.user.getUsername())).thenReturn(sDTO);

        Assertions.assertIterableEquals(sDTO, this.watchlistController.getStockWatchList(this.user).getBody());
    }

    @Test
    public void deleteCryptoWatch() {
        doNothing().when(this.watchlistService).deleteCryptoWatchById(this.user.getUsername(), 1L);

        Assertions.assertEquals(HttpStatus.OK, this.watchlistController.deleteCryptoWatch(user, 1L).getStatusCode());
    }

    @Test
    public void deleteStockWatch() {
        doNothing().when(this.watchlistService).deleteStockWatchById(this.user.getUsername(), 1L);

        Assertions.assertEquals(HttpStatus.OK, this.watchlistController.deleteStockWatch(user, 1L).getStatusCode());
    }

    @Test
    public void deleteForexWatch() {
        doNothing().when(this.watchlistService).deleteForexWatchById(this.user.getUsername(), 1L);

        Assertions.assertEquals(HttpStatus.OK, this.watchlistController.deleteForexWatch(user, 1L).getStatusCode());
    }

    @Test
    public void createStockWatch() {
        Stock stock = Stock.builder().shortName("s1").exchange("e1").id("i1").build();
        StockWatchDTO sDTO = StockWatchDTO.builder().tickerWatchId(1L).liveValue(32.1).change(33.1).stockExchange("e1").stockName("n1").stockShortName("s1").pChange(3.1).build();

        when(this.watchlistService.createNewStockWatch(this.user.getUsername(), stock)).thenReturn(sDTO);

        Assertions.assertEquals(sDTO, this.watchlistController.createStockWatch(this.user, stock).getBody());
    }

    @Test
    public void createCryptoWatch() {
        CryptoWatchDTO cDTO = CryptoWatchDTO.builder().cryptoWatchId(1L).liveValue(32.1).change(33.1).coinId("c1").name("n1").symbol("s1").build();

        when(this.watchlistService.createNewCryptoWatch(this.user.getUsername(), "c1")).thenReturn(cDTO);

        Assertions.assertEquals(cDTO, this.watchlistController.createCryptoWatch(this.user, "c1").getBody());
    }
}