package eye.on.the.money.controller;

import eye.on.the.money.dto.in.WatchGroupEditDTO;
import eye.on.the.money.dto.out.CryptoWatchDTO;
import eye.on.the.money.dto.out.ForexWatchDTO;
import eye.on.the.money.dto.out.StockWatchDTO;
import eye.on.the.money.dto.out.WatchGroupDTO;
import eye.on.the.money.model.User;
import eye.on.the.money.model.stock.Stock;
import eye.on.the.money.service.shared.WatchListService;
import eye.on.the.money.service.watchlist.WatchGroupService;
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

    @Mock
    private WatchGroupService watchGroupService;

    @InjectMocks
    private WatchlistController watchlistController;

    private final User user = User.builder().id(1L).email("email").build();

    @Test
    public void getCryptoWatchList() {
        List<CryptoWatchDTO> cDTO = new ArrayList<>();
        cDTO.add(CryptoWatchDTO.builder().cryptoWatchId(1L).liveValue(32.1).change(33.1).coinId("c1").name("n1").symbol("s1").build());
        cDTO.add(CryptoWatchDTO.builder().cryptoWatchId(2L).liveValue(632.1).change(333.1).coinId("c2").name("n2").symbol("s2").build());
        cDTO.add(CryptoWatchDTO.builder().cryptoWatchId(3L).liveValue(32.3).change(11.1).coinId("c3").name("n3").symbol("s3").build());

        when(this.watchlistService.getCryptoWatchlistByUserId(this.user.getId(), "eur")).thenReturn(cDTO);

        Assertions.assertIterableEquals(cDTO, this.watchlistController.getCryptoWatchList(1L, "eur").getBody());
    }

    @Test
    public void getForexWatchList() {
        List<ForexWatchDTO> fDTO = new ArrayList<>();
        fDTO.add(ForexWatchDTO.builder().forexWatchID(1L).liveValue(32.1).change(33.1).pChange(3.1).fromCurrencyId("fc1").toCurrencyId("tc1").build());
        fDTO.add(ForexWatchDTO.builder().forexWatchID(2L).liveValue(632.1).change(333.1).pChange(5.1).fromCurrencyId("fc2").toCurrencyId("tc2").build());
        fDTO.add(ForexWatchDTO.builder().forexWatchID(3L).liveValue(32.3).change(11.1).pChange(3.3).fromCurrencyId("fc3").toCurrencyId("tc3").build());

        when(this.watchlistService.getForexWatchlistByUserId(this.user.getId())).thenReturn(fDTO);

        Assertions.assertIterableEquals(fDTO, this.watchlistController.getForexWatchList(1L).getBody());
    }

    @Test
    public void getStockWatchList() {
        List<StockWatchDTO> sDTO = new ArrayList<>();
        sDTO.add(StockWatchDTO.builder().tickerWatchId(1L).liveValue(32.1).change(33.1).stockExchange("e1").stockName("n1").stockShortName("s1").pChange(3.1).build());
        sDTO.add(StockWatchDTO.builder().tickerWatchId(2L).liveValue(632.1).change(333.1).stockExchange("e2").stockName("n2").stockShortName("s2").pChange(1.1).build());
        sDTO.add(StockWatchDTO.builder().tickerWatchId(3L).liveValue(32.3).change(11.1).stockExchange("e3").stockName("n3").stockShortName("s3").pChange(3.3).build());

        when(this.watchlistService.getStockWatchlistByUserId(this.user.getId())).thenReturn(sDTO);

        Assertions.assertIterableEquals(sDTO, this.watchlistController.getStockWatchList(1L).getBody());
    }

    @Test
    public void deleteCryptoWatch() {
        doNothing().when(this.watchlistService).deleteCryptoWatchById(this.user.getId(), 1L);

        Assertions.assertEquals(HttpStatus.OK, this.watchlistController.deleteCryptoWatch(1L, 1L).getStatusCode());
    }

    @Test
    public void deleteStockWatch() {
        doNothing().when(this.watchlistService).deleteStockWatchById(this.user.getId(), 1L);

        Assertions.assertEquals(HttpStatus.OK, this.watchlistController.deleteStockWatch(1L, 1L).getStatusCode());
    }

    @Test
    public void deleteForexWatch() {
        doNothing().when(this.watchlistService).deleteForexWatchById(this.user.getId(), 1L);

        Assertions.assertEquals(HttpStatus.OK, this.watchlistController.deleteForexWatch(1L, 1L).getStatusCode());
    }

    @Test
    public void createStockWatch() {
        Stock stock = Stock.builder().shortName("s1").exchange("e1").id("i1").build();
        StockWatchDTO sDTO = StockWatchDTO.builder().tickerWatchId(1L).liveValue(32.1).change(33.1).stockExchange("e1").stockName("n1").stockShortName("s1").pChange(3.1).build();

        when(this.watchlistService.createNewStockWatch(this.user.getId(), stock, null)).thenReturn(sDTO);

        Assertions.assertEquals(sDTO, this.watchlistController.createStockWatch(1L, stock, null).getBody());
    }

    @Test
    public void createCryptoWatch() {
        CryptoWatchDTO cDTO = CryptoWatchDTO.builder().cryptoWatchId(1L).liveValue(32.1).change(33.1).coinId("c1").name("n1").symbol("s1").build();

        when(this.watchlistService.createNewCryptoWatch(this.user.getId(), "c1")).thenReturn(cDTO);

        Assertions.assertEquals(cDTO, this.watchlistController.createCryptoWatch(1L, "c1").getBody());
    }

    @Test
    public void createStockWatchInAGroup() {
        Stock stock = Stock.builder().shortName("s1").exchange("e1").id("i1").build();
        StockWatchDTO sDTO = StockWatchDTO.builder().tickerWatchId(1L).stockShortName("s1").groupId(5L).groupName("Tech").build();

        when(this.watchlistService.createNewStockWatch(this.user.getId(), stock, 5L)).thenReturn(sDTO);

        Assertions.assertEquals(sDTO, this.watchlistController.createStockWatch(1L, stock, 5L).getBody());
    }

    @Test
    public void setStockWatchGroup() {
        StockWatchDTO sDTO = StockWatchDTO.builder().tickerWatchId(1L).groupId(5L).groupName("Tech").build();

        when(this.watchlistService.setStockWatchGroup(this.user.getId(), 1L, 5L)).thenReturn(sDTO);

        Assertions.assertEquals(sDTO, this.watchlistController.setStockWatchGroup(1L, 1L, 5L).getBody());
    }

    @Test
    public void setStockWatchGroupClearsItWhenNoGroupIsGiven() {
        StockWatchDTO sDTO = StockWatchDTO.builder().tickerWatchId(1L).build();

        when(this.watchlistService.setStockWatchGroup(this.user.getId(), 1L, null)).thenReturn(sDTO);

        Assertions.assertNull(this.watchlistController.setStockWatchGroup(1L, 1L, null).getBody().getGroupId());
    }

    @Test
    public void getGroups() {
        List<WatchGroupDTO> groups = List.of(WatchGroupDTO.builder().id(1L).name("Tech").build());

        when(this.watchGroupService.getGroups(this.user.getId())).thenReturn(groups);

        Assertions.assertEquals(groups, this.watchlistController.getGroups(1L).getBody());
    }

    @Test
    public void createGroup() {
        WatchGroupEditDTO request = new WatchGroupEditDTO("Tech");
        WatchGroupDTO saved = WatchGroupDTO.builder().id(1L).name("Tech").build();

        when(this.watchGroupService.createGroup(this.user.getId(), request)).thenReturn(saved);

        Assertions.assertEquals(saved, this.watchlistController.createGroup(1L, request).getBody());
    }

    @Test
    public void updateGroup() {
        WatchGroupEditDTO request = new WatchGroupEditDTO("Technology");
        WatchGroupDTO saved = WatchGroupDTO.builder().id(1L).name("Technology").build();

        when(this.watchGroupService.updateGroup(this.user.getId(), 1L, request)).thenReturn(saved);

        Assertions.assertEquals(saved, this.watchlistController.updateGroup(1L, 1L, request).getBody());
    }

    @Test
    public void deleteGroupAnswers404WhenThereWasNothingToDelete() {
        when(this.watchGroupService.deleteGroup(this.user.getId(), 1L)).thenReturn(true);
        when(this.watchGroupService.deleteGroup(this.user.getId(), 2L)).thenReturn(false);

        Assertions.assertEquals(HttpStatus.OK, this.watchlistController.deleteGroup(1L, 1L).getStatusCode());
        Assertions.assertEquals(HttpStatus.NOT_FOUND, this.watchlistController.deleteGroup(1L, 2L).getStatusCode());
    }
}
