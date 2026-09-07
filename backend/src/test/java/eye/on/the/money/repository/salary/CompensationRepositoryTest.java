package eye.on.the.money.repository.salary;

import eye.on.the.money.EotmApplication;
import eye.on.the.money.model.User;
import eye.on.the.money.model.salary.CompensationAmountMode;
import eye.on.the.money.model.salary.CompensationItem;
import eye.on.the.money.model.salary.CompensationTaxTreatment;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = EotmApplication.class)
@ActiveProfiles("test")
@Transactional
class CompensationRepositoryTest {

    private static final String USER_EMAIL = "test@test.test";

    @Autowired
    private CompensationRepository compensationRepository;

    @Autowired
    private UserService userService;

    @PersistenceContext
    private EntityManager entityManager;

    private User user;

    @BeforeEach
    void setUp() {
        this.compensationRepository.deleteAll();
        this.user = this.userService.loadUserByEmail(USER_EMAIL);
    }

    private CompensationItem save(String name, String monthlyAmount) {
        return this.compensationRepository.saveAndFlush(CompensationItem.builder()
                .name(name)
                .amountMode(CompensationAmountMode.MONTHLY_AMOUNT)
                .monthlyAmount(new BigDecimal(monthlyAmount))
                .taxTreatment(CompensationTaxTreatment.TAX_FREE)
                .user(this.user)
                .build());
    }

    @Test
    void findByUserIdOrderByNameAscIdAsc_ordersByTheNameYouGaveTheItem() {
        this.save("Phone", "8000");
        this.save("Annual bonus", "100000");

        List<CompensationItem> items = this.compensationRepository
                .findByUserIdOrderByNameAscIdAsc(this.user.getId());

        assertEquals(2, items.size());
        assertEquals("Annual bonus", items.get(0).getName());
        assertEquals("Phone", items.get(1).getName());
    }

    @Test
    void save_keepsAPercentageItemWithoutAMonthlyAmount() {
        CompensationItem saved = this.compensationRepository.saveAndFlush(CompensationItem.builder()
                .name("Annual bonus")
                .amountMode(CompensationAmountMode.PERCENT_OF_ANNUAL)
                .percent(new BigDecimal("15"))
                .taxTreatment(CompensationTaxTreatment.TAXED_AS_SALARY)
                .user(this.user)
                .build());
        this.entityManager.flush();

        CompensationItem found = this.compensationRepository.findById(saved.getId()).orElseThrow();

        assertNull(found.getMonthlyAmount());
        assertEquals(0, new BigDecimal("15").compareTo(found.getPercent()));
    }

    @Test
    void findByIdAndUserId_doesNotReturnAnotherUsersItem() {
        CompensationItem item = this.save("Phone", "8000");

        assertTrue(this.compensationRepository.findByIdAndUserId(item.getId(), this.user.getId()).isPresent());
        assertTrue(this.compensationRepository.findByIdAndUserId(item.getId(), -1L).isEmpty());
    }

    @Test
    void deleteByUserIdAndIdIn_ignoresAnotherUsersItem() {
        CompensationItem item = this.save("Phone", "8000");

        this.compensationRepository.deleteByUserIdAndIdIn(-1L, List.of(item.getId()));
        this.entityManager.flush();

        assertTrue(this.compensationRepository.findById(item.getId()).isPresent());
    }
}
