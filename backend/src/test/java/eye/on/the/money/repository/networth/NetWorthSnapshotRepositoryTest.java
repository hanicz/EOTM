package eye.on.the.money.repository.networth;

import eye.on.the.money.EotmApplication;
import eye.on.the.money.model.User;
import eye.on.the.money.model.networth.NetWorthSnapshot;
import eye.on.the.money.service.user.UserService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = EotmApplication.class)
@ActiveProfiles("test")
@Transactional
class NetWorthSnapshotRepositoryTest {

    private static final String USER_EMAIL = "test@test.test";

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
    void findByUserIdOrderBySnapshotDate_returnsTheRowsOldestFirst() {
        this.netWorthSnapshotRepository.saveAllAndFlush(List.of(
                this.snapshot(LocalDate.of(2026, 5, 3), "Stock"),
                this.snapshot(LocalDate.of(2026, 5, 1), "Stock"),
                this.snapshot(LocalDate.of(2026, 5, 2), "Stock")));
        this.entityManager.clear();

        List<NetWorthSnapshot> found = this.netWorthSnapshotRepository.findByUserIdOrderBySnapshotDate(this.user.getId());

        assertEquals(List.of(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 2), LocalDate.of(2026, 5, 3)),
                found.stream().map(NetWorthSnapshot::getSnapshotDate).toList());
    }

    @Test
    void findByUserIdAndSnapshotDate_returnsOnlyThatDay() {
        this.netWorthSnapshotRepository.saveAllAndFlush(List.of(
                this.snapshot(LocalDate.of(2026, 5, 1), "Stock"),
                this.snapshot(LocalDate.of(2026, 5, 1), "Cash"),
                this.snapshot(LocalDate.of(2026, 5, 2), "Stock")));
        this.entityManager.clear();

        assertEquals(2, this.netWorthSnapshotRepository
                .findByUserIdAndSnapshotDate(this.user.getId(), LocalDate.of(2026, 5, 1)).size());
        assertTrue(this.netWorthSnapshotRepository.existsByUserIdAndSnapshotDate(this.user.getId(), LocalDate.of(2026, 5, 2)));
        assertFalse(this.netWorthSnapshotRepository.existsByUserIdAndSnapshotDate(this.user.getId(), LocalDate.of(2026, 5, 3)));
    }

    @Test
    void deleteByUserIdAndSnapshotDate_removesOnlyThatDay() {
        this.netWorthSnapshotRepository.saveAllAndFlush(List.of(
                this.snapshot(LocalDate.of(2026, 5, 1), "Stock"),
                this.snapshot(LocalDate.of(2026, 5, 1), "Cash"),
                this.snapshot(LocalDate.of(2026, 5, 2), "Stock")));

        int deleted = this.netWorthSnapshotRepository.deleteByUserIdAndSnapshotDate(this.user.getId(),
                LocalDate.of(2026, 5, 1));

        assertEquals(2, deleted);
        assertFalse(this.netWorthSnapshotRepository.existsByUserIdAndSnapshotDate(this.user.getId(), LocalDate.of(2026, 5, 1)));
        assertTrue(this.netWorthSnapshotRepository.existsByUserIdAndSnapshotDate(this.user.getId(), LocalDate.of(2026, 5, 2)));
    }

    @Test
    void save_rejectsASecondRowForTheSameDayAndAssetClass() {
        this.netWorthSnapshotRepository.saveAndFlush(this.snapshot(LocalDate.of(2026, 5, 1), "Stock"));

        assertThrows(DataIntegrityViolationException.class, () ->
                this.netWorthSnapshotRepository.saveAndFlush(this.snapshot(LocalDate.of(2026, 5, 1), "Stock")));
    }

    private NetWorthSnapshot snapshot(LocalDate date, String assetClass) {
        return NetWorthSnapshot.builder()
                .user(this.user)
                .snapshotDate(date)
                .assetClass(assetClass)
                .spent(1000.0)
                .worth(1200.0)
                .build();
    }
}
