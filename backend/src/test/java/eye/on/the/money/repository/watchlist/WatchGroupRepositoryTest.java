package eye.on.the.money.repository.watchlist;

import eye.on.the.money.EotmApplication;
import eye.on.the.money.model.User;
import eye.on.the.money.model.stock.Stock;
import eye.on.the.money.model.watchlist.TickerWatch;
import eye.on.the.money.model.watchlist.WatchGroup;
import eye.on.the.money.repository.stock.StockRepository;
import eye.on.the.money.service.user.UserService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = EotmApplication.class)
@ActiveProfiles("test")
@Transactional
class WatchGroupRepositoryTest {

    private static final String USER_EMAIL = "test@test.test";

    @Autowired
    private WatchGroupRepository watchGroupRepository;

    @Autowired
    private StockWatchRepository stockWatchRepository;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private UserService userService;

    @PersistenceContext
    private EntityManager entityManager;

    private User user;

    @BeforeEach
    void setUp() {
        this.stockWatchRepository.deleteAll();
        this.watchGroupRepository.deleteAll();
        this.entityManager.flush();
        this.user = this.userService.loadUserByEmail(USER_EMAIL);
    }

    private WatchGroup group(String name) {
        return this.watchGroupRepository.saveAndFlush(
                WatchGroup.builder().name(name).user(this.user).build());
    }

    private TickerWatch watch(String shortName, WatchGroup group) {
        Stock stock = this.stockRepository.saveAndFlush(Stock.builder()
                .id(shortName.toLowerCase() + ".us").shortName(shortName).exchange("US")
                .name(shortName + " Inc.").build());
        return this.stockWatchRepository.saveAndFlush(
                TickerWatch.builder().stock(stock).user(this.user).group(group).build());
    }

    @Test
    void storesAndReadsBackTheGroupOfAWatch() {
        WatchGroup tech = this.group("Tech");
        this.watch("FIXA", tech);
        this.entityManager.clear();

        List<TickerWatch> watches = this.stockWatchRepository.findByUserIdOrderByStockShortName(this.user.getId());

        assertEquals(1, watches.size());
        assertEquals("Tech", watches.getFirst().getGroup().getName());
    }

    @Test
    void leavesTheGroupNullForAnUngroupedWatch() {
        this.watch("FIXB", null);
        this.entityManager.clear();

        TickerWatch found = this.stockWatchRepository.findByUserIdOrderByStockShortName(this.user.getId()).getFirst();

        assertNull(found.getGroup());
    }

    @Test
    void deletingAGroupTakesItsWatchesWithItAndLeavesTheRestAlone() {
        WatchGroup tech = this.group("Tech");
        this.watch("FIXC", tech);
        this.watch("FIXD", null);
        this.entityManager.flush();
        this.entityManager.clear();

        WatchGroup reloaded = this.watchGroupRepository.findByUserIdAndId(this.user.getId(), tech.getId())
                .orElseThrow();
        this.watchGroupRepository.delete(reloaded);
        this.entityManager.flush();
        this.entityManager.clear();

        List<TickerWatch> remaining = this.stockWatchRepository.findByUserIdOrderByStockShortName(this.user.getId());

        assertEquals(1, remaining.size());
        assertEquals("FIXD", remaining.getFirst().getStock().getShortName());
        assertTrue(this.watchGroupRepository.findByUserIdAndId(this.user.getId(), tech.getId()).isEmpty());
    }

    @Test
    void findsAGroupByItsExactName() {
        this.group("Tech");
        this.entityManager.clear();

        assertTrue(this.watchGroupRepository.findByUserIdAndName(this.user.getId(), "Tech").isPresent());
        assertTrue(this.watchGroupRepository.findByUserIdAndName(this.user.getId(), "tech").isEmpty());
    }
}
