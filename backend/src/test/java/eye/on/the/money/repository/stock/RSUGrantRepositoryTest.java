package eye.on.the.money.repository.stock;

import eye.on.the.money.EotmApplication;
import eye.on.the.money.model.User;
import eye.on.the.money.model.stock.RSUGrant;
import eye.on.the.money.model.stock.VestingFrequency;
import eye.on.the.money.service.user.UserService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = EotmApplication.class)
@ActiveProfiles("test")
@Transactional
class RSUGrantRepositoryTest {

    private static final String USER_EMAIL = "test@test.test";

    @Autowired
    private RSUGrantRepository rsuGrantRepository;

    @Autowired
    private UserService userService;

    @PersistenceContext
    private EntityManager entityManager;

    private User user;

    @BeforeEach
    void setUp() {
        this.rsuGrantRepository.deleteAll();
        this.user = this.userService.loadUserByEmail(USER_EMAIL);
    }

    private RSUGrant save(String shortName, LocalDate grantDate) {
        return this.rsuGrantRepository.saveAndFlush(RSUGrant.builder()
                .shortName(shortName)
                .exchange("US")
                .grantDate(grantDate)
                .quantity(400)
                .vestingYears(4)
                .vestingFrequency(VestingFrequency.ANNUAL)
                .user(this.user)
                .build());
    }

    @Test
    void findByUserIdOrderByGrantDateDescIdAsc_putsTheNewestGrantFirst() {
        this.save("ACME", LocalDate.of(2021, 3, 1));
        this.save("WIDGET", LocalDate.of(2024, 3, 1));

        List<RSUGrant> grants = this.rsuGrantRepository
                .findByUserIdOrderByGrantDateDescIdAsc(this.user.getId());

        assertEquals(2, grants.size());
        assertEquals("WIDGET", grants.get(0).getShortName());
        assertEquals("ACME", grants.get(1).getShortName());
    }

    @Test
    void save_keepsAGrantWithoutACurrencyOverrideOrNote() {
        RSUGrant saved = this.save("ACME", LocalDate.of(2021, 3, 1));
        this.entityManager.flush();

        RSUGrant found = this.rsuGrantRepository.findById(saved.getId()).orElseThrow();

        assertNull(found.getCurrency());
        assertNull(found.getNote());
        assertEquals(VestingFrequency.ANNUAL, found.getVestingFrequency());
        assertEquals(4, found.getVestingYears());
    }

    @Test
    void save_keepsAQuarterlyGrantWithACurrencyOverride() {
        RSUGrant saved = this.rsuGrantRepository.saveAndFlush(RSUGrant.builder()
                .shortName("WIDGET").exchange("LSE").currency("GBP")
                .grantDate(LocalDate.of(2024, 3, 1)).quantity(80).vestingYears(2)
                .vestingFrequency(VestingFrequency.QUARTERLY).note("second grant")
                .user(this.user).build());
        this.entityManager.flush();

        RSUGrant found = this.rsuGrantRepository.findById(saved.getId()).orElseThrow();

        assertEquals("GBP", found.getCurrency());
        assertEquals("second grant", found.getNote());
        assertEquals(VestingFrequency.QUARTERLY, found.getVestingFrequency());
    }

    @Test
    void findByIdAndUserId_doesNotReturnAnotherUsersGrant() {
        RSUGrant grant = this.save("ACME", LocalDate.of(2021, 3, 1));

        assertTrue(this.rsuGrantRepository.findByIdAndUserId(grant.getId(), this.user.getId()).isPresent());
        assertTrue(this.rsuGrantRepository.findByIdAndUserId(grant.getId(), -1L).isEmpty());
    }

    @Test
    void deleteByUserIdAndIdIn_ignoresAnotherUsersGrant() {
        RSUGrant grant = this.save("ACME", LocalDate.of(2021, 3, 1));

        this.rsuGrantRepository.deleteByUserIdAndIdIn(-1L, List.of(grant.getId()));
        this.entityManager.flush();

        assertTrue(this.rsuGrantRepository.findById(grant.getId()).isPresent());
    }
}
