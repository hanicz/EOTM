package eye.on.the.money.repository.financial;

import eye.on.the.money.EotmApplication;
import eye.on.the.money.model.User;
import eye.on.the.money.model.financial.AccountSide;
import eye.on.the.money.model.financial.BankExclusionRule;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = EotmApplication.class)
@ActiveProfiles("test")
@Transactional
class BankExclusionRuleRepositoryTest {

    private static final String USER_EMAIL = "test@test.test";
    private static final String ACCOUNT = "12001008-00000000-00000001";
    private static final String NORMALIZED = "120010080000000000000001";

    @Autowired
    private BankExclusionRuleRepository bankExclusionRuleRepository;

    @Autowired
    private UserService userService;

    @PersistenceContext
    private EntityManager entityManager;

    private User user;

    @BeforeEach
    void setUp() {
        this.bankExclusionRuleRepository.deleteAll();
        this.user = this.userService.loadUserByEmail(USER_EMAIL);
    }

    private BankExclusionRule save(String accountNumber, String normalized, AccountSide side, boolean active) {
        return this.bankExclusionRuleRepository.saveAndFlush(BankExclusionRule.builder()
                .accountNumber(accountNumber)
                .normalizedAccount(normalized)
                .side(side)
                .active(active)
                .creationDate(LocalDate.now())
                .user(this.user)
                .build());
    }

    @Test
    void findByUserIdAndActiveTrue_leavesOutInactiveRules() {
        this.save(ACCOUNT, NORMALIZED, AccountSide.PARTNER_ACCOUNT, true);
        this.save("1111-1111", "11111111", AccountSide.OWN_ACCOUNT, false);

        List<BankExclusionRule> active = this.bankExclusionRuleRepository.findByUserIdAndActiveTrue(this.user.getId());

        assertEquals(1, active.size());
        assertEquals(NORMALIZED, active.get(0).getNormalizedAccount());
    }

    @Test
    void findByUserIdOrderByAccountNumberAsc_returnsEveryRuleOfTheUser() {
        this.save("B-ACCOUNT", "BACCOUNT", AccountSide.ANY, true);
        this.save("A-ACCOUNT", "AACCOUNT", AccountSide.ANY, false);

        List<BankExclusionRule> rules =
                this.bankExclusionRuleRepository.findByUserIdOrderByAccountNumberAsc(this.user.getId());

        assertEquals(2, rules.size());
        assertEquals("A-ACCOUNT", rules.get(0).getAccountNumber());
        assertEquals("B-ACCOUNT", rules.get(1).getAccountNumber());
    }

    @Test
    void findByIdAndUserId_doesNotReturnAnotherUsersRule() {
        BankExclusionRule rule = this.save(ACCOUNT, NORMALIZED, AccountSide.ANY, true);

        assertTrue(this.bankExclusionRuleRepository.findByIdAndUserId(rule.getId(), this.user.getId()).isPresent());
        assertTrue(this.bankExclusionRuleRepository.findByIdAndUserId(rule.getId(), -1L).isEmpty());
    }

    @Test
    void findByUserIdAndNormalizedAccountAndSide_isScopedToTheUser() {
        this.save(ACCOUNT, NORMALIZED, AccountSide.PARTNER_ACCOUNT, true);

        Optional<BankExclusionRule> mine = this.bankExclusionRuleRepository
                .findByUserIdAndNormalizedAccountAndSide(this.user.getId(), NORMALIZED, AccountSide.PARTNER_ACCOUNT);
        Optional<BankExclusionRule> theirs = this.bankExclusionRuleRepository
                .findByUserIdAndNormalizedAccountAndSide(-1L, NORMALIZED, AccountSide.PARTNER_ACCOUNT);

        assertTrue(mine.isPresent());
        assertTrue(theirs.isEmpty());
    }

    @Test
    void deleteByUserIdAndIdIn_ignoresAnotherUsersRule() {
        BankExclusionRule rule = this.save(ACCOUNT, NORMALIZED, AccountSide.ANY, true);

        this.bankExclusionRuleRepository.deleteByUserIdAndIdIn(-1L, List.of(rule.getId()));
        this.entityManager.flush();

        assertTrue(this.bankExclusionRuleRepository.findById(rule.getId()).isPresent());
    }

    @Test
    void save_rejectsTheSameAccountAndSideTwice() {
        this.save(ACCOUNT, NORMALIZED, AccountSide.PARTNER_ACCOUNT, true);

        assertThrows(DataIntegrityViolationException.class,
                () -> this.save(ACCOUNT, NORMALIZED, AccountSide.PARTNER_ACCOUNT, false));
    }

    @Test
    void save_allowsTheSameAccountOnDifferentSides() {
        this.save(ACCOUNT, NORMALIZED, AccountSide.PARTNER_ACCOUNT, true);
        this.save(ACCOUNT, NORMALIZED, AccountSide.OWN_ACCOUNT, true);

        assertEquals(2, this.bankExclusionRuleRepository
                .findByUserIdOrderByAccountNumberAsc(this.user.getId()).size());
    }
}
