package eye.on.the.money.repository.cash;

import eye.on.the.money.EotmApplication;
import eye.on.the.money.model.Currency;
import eye.on.the.money.model.User;
import eye.on.the.money.model.cash.Cash;
import eye.on.the.money.repository.forex.CurrencyRepository;
import eye.on.the.money.service.user.UserService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = EotmApplication.class)
@ActiveProfiles("test")
@Transactional
class CashRepositoryTest {

    private static final String USER_EMAIL = "test@test.test";

    @Autowired
    private CashRepository cashRepository;

    @Autowired
    private CurrencyRepository currencyRepository;

    @Autowired
    private UserService userService;

    @PersistenceContext
    private EntityManager entityManager;

    private User user;

    @BeforeEach
    void setUp() {
        this.cashRepository.deleteAll();
        this.entityManager.flush();
        this.user = this.userService.loadUserByEmail(USER_EMAIL);
    }

    @Test
    void storesAndReadsBackTheCurrency() {
        Currency eur = this.currencyRepository.findById("EUR").orElseThrow();
        this.cashRepository.saveAndFlush(
                Cash.builder().amount(1500.0).currency(eur).user(this.user).build());
        this.entityManager.clear();

        Optional<Cash> found = this.cashRepository.findByUserId(this.user.getId());

        assertTrue(found.isPresent());
        assertEquals(1500.0, found.get().getAmount());
        assertEquals("EUR", found.get().getCurrency().getId());
    }

    @Test
    void changingTheCurrencyLeavesTheAmountAlone() {
        Currency huf = this.currencyRepository.findById("HUF").orElseThrow();
        Currency usd = this.currencyRepository.findById("USD").orElseThrow();
        Cash cash = this.cashRepository.saveAndFlush(
                Cash.builder().amount(750000.0).currency(huf).user(this.user).build());

        cash.setCurrency(usd);
        this.cashRepository.saveAndFlush(cash);
        this.entityManager.clear();

        Cash found = this.cashRepository.findByUserId(this.user.getId()).orElseThrow();

        assertEquals(750000.0, found.getAmount());
        assertEquals("USD", found.getCurrency().getId());
    }
}
