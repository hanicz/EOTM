package eye.on.the.money.service.shared;

import eye.on.the.money.EotmApplication;
import eye.on.the.money.dto.out.AssetClassValueDTO;
import eye.on.the.money.model.User;
import eye.on.the.money.model.networth.NetWorthSnapshot;
import eye.on.the.money.repository.networth.NetWorthSnapshotRepository;
import eye.on.the.money.service.user.UserService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = EotmApplication.class)
@ActiveProfiles("test")
@Transactional
class NetWorthSnapshotWriterTest {

    private static final String USER_EMAIL = "test@test.test";
    private static final LocalDate DAY = LocalDate.of(2026, 6, 10);

    @Autowired
    private NetWorthSnapshotWriter netWorthSnapshotWriter;

    @Autowired
    private NetWorthSnapshotRepository netWorthSnapshotRepository;

    @Autowired
    private UserService userService;

    @PersistenceContext
    private EntityManager entityManager;

    private User user;

    @BeforeEach
    void setUp() {
        this.netWorthSnapshotRepository.deleteAll();
        this.user = this.userService.loadUserByEmail(USER_EMAIL);
    }

    @Test
    void replace_overwritesTheDayAndDropsAssetClassesNoLongerPresent() {
        this.netWorthSnapshotRepository.saveAllAndFlush(List.of(
                this.snapshot(DAY, "Stock", 100, 110),
                this.snapshot(DAY, "Crypto", 50, 40)));

        this.netWorthSnapshotWriter.replace(this.user.getId(), DAY,
                List.of(this.asset("Stock", 200, 260), this.asset("Cash", 70, 70)));
        this.entityManager.flush();
        this.entityManager.clear();

        Map<String, NetWorthSnapshot> stored = this.storedOn(DAY);
        assertEquals(2, stored.size());
        assertDecimal(200.0, stored.get("Stock").getSpent());
        assertDecimal(260.0, stored.get("Stock").getWorth());
        assertDecimal(70.0, stored.get("Cash").getWorth());
    }

    @Test
    void replace_leavesOtherDaysUntouched() {
        LocalDate previous = DAY.minusDays(1);
        this.netWorthSnapshotRepository.saveAllAndFlush(List.of(
                this.snapshot(previous, "Stock", 100, 110),
                this.snapshot(DAY, "Stock", 100, 120)));

        this.netWorthSnapshotWriter.replace(this.user.getId(), DAY, List.of(this.asset("Stock", 150, 180)));
        this.entityManager.flush();
        this.entityManager.clear();

        assertDecimal(110.0, this.storedOn(previous).get("Stock").getWorth());
        assertDecimal(180.0, this.storedOn(DAY).get("Stock").getWorth());
    }

    @Test
    void insertIfMissing_doesNothingWhenTheDayAlreadyExists() {
        this.netWorthSnapshotRepository.saveAllAndFlush(List.of(this.snapshot(DAY, "Stock", 100, 110)));

        this.netWorthSnapshotWriter.insertIfMissing(this.user.getId(), DAY,
                List.of(this.asset("Stock", 999, 999), this.asset("Cash", 5, 5)));
        this.entityManager.flush();
        this.entityManager.clear();

        Map<String, NetWorthSnapshot> stored = this.storedOn(DAY);
        assertEquals(1, stored.size());
        assertDecimal(110.0, stored.get("Stock").getWorth());
    }

    @Test
    void insertIfMissing_storesOneRowPerAssetClass() {
        this.netWorthSnapshotWriter.insertIfMissing(this.user.getId(), DAY,
                List.of(this.asset("Stock", 300, 330), this.asset("Cash", 40, 40)));
        this.entityManager.flush();
        this.entityManager.clear();

        Map<String, NetWorthSnapshot> stored = this.storedOn(DAY);
        assertEquals(2, stored.size());
        assertDecimal(300.0, stored.get("Stock").getSpent());
        assertDecimal(330.0, stored.get("Stock").getWorth());
        assertDecimal(40.0, stored.get("Cash").getSpent());
    }

    private static void assertDecimal(double expected, BigDecimal actual) {
        assertEquals(0, BigDecimal.valueOf(expected).compareTo(actual), () -> "expected " + expected + " but was " + actual);
    }

    private Map<String, NetWorthSnapshot> storedOn(LocalDate date) {
        return this.netWorthSnapshotRepository.findByUserIdAndSnapshotDate(this.user.getId(), date).stream()
                .collect(Collectors.toMap(NetWorthSnapshot::getAssetClass, row -> row));
    }

    private AssetClassValueDTO asset(String assetClass, double spent, double worth) {
        return AssetClassValueDTO.builder()
                .assetClass(assetClass)
                .spent(BigDecimal.valueOf(spent))
                .worth(BigDecimal.valueOf(worth))
                .build();
    }

    private NetWorthSnapshot snapshot(LocalDate date, String assetClass, double spent, double worth) {
        return NetWorthSnapshot.builder()
                .user(this.user)
                .snapshotDate(date)
                .assetClass(assetClass)
                .spent(BigDecimal.valueOf(spent))
                .worth(BigDecimal.valueOf(worth))
                .build();
    }
}
