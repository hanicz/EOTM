package eye.on.the.money.repository.salary;

import eye.on.the.money.EotmApplication;
import eye.on.the.money.model.Currency;
import eye.on.the.money.model.User;
import eye.on.the.money.model.salary.Salary;
import eye.on.the.money.model.salary.SalaryBasis;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = EotmApplication.class)
@ActiveProfiles("test")
@Transactional
class SalaryRepositoryTest {

    private static final String USER_EMAIL = "test@test.test";

    @Autowired
    private SalaryRepository salaryRepository;

    @Autowired
    private CurrencyRepository currencyRepository;

    @Autowired
    private UserService userService;

    @PersistenceContext
    private EntityManager entityManager;

    private User user;
    private Currency huf;

    @BeforeEach
    void setUp() {
        this.salaryRepository.deleteAll();
        this.user = this.userService.loadUserByEmail(USER_EMAIL);
        this.huf = this.currencyRepository.findById("HUF").orElseThrow();
    }

    private Salary save(String amount, LocalDate validFrom, LocalDate validTo) {
        return this.salaryRepository.saveAndFlush(Salary.builder()
                .amount(new BigDecimal(amount))
                .basis(SalaryBasis.MONTHLY)
                .validFrom(validFrom)
                .validTo(validTo)
                .dependents(0)
                .currency(this.huf)
                .user(this.user)
                .build());
    }

    @Test
    void findByUserIdOrderByValidFromDesc_putsTheNewestPeriodFirst() {
        this.save("400000", LocalDate.of(2021, 1, 1), LocalDate.of(2023, 12, 31));
        this.save("900000", LocalDate.of(2024, 1, 1), null);

        List<Salary> salaries = this.salaryRepository.findByUserIdOrderByValidFromDesc(this.user.getId());

        assertEquals(2, salaries.size());
        assertEquals(LocalDate.of(2024, 1, 1), salaries.get(0).getValidFrom());
        assertNull(salaries.get(0).getValidTo());
        assertEquals(LocalDate.of(2021, 1, 1), salaries.get(1).getValidFrom());
    }

    @Test
    void findByIdAndUserId_doesNotReturnAnotherUsersSalary() {
        Salary salary = this.save("600000", LocalDate.of(2024, 6, 1), null);

        assertTrue(this.salaryRepository.findByIdAndUserId(salary.getId(), this.user.getId()).isPresent());
        assertTrue(this.salaryRepository.findByIdAndUserId(salary.getId(), -1L).isEmpty());
    }

    @Test
    void deleteByUserIdAndIdIn_ignoresAnotherUsersSalary() {
        Salary salary = this.save("600000", LocalDate.of(2024, 6, 1), null);

        this.salaryRepository.deleteByUserIdAndIdIn(-1L, List.of(salary.getId()));
        this.entityManager.flush();

        assertTrue(this.salaryRepository.findById(salary.getId()).isPresent());
    }

    @Test
    void save_keepsOverlappingPeriodsForConcurrentJobs() {
        this.save("600000", LocalDate.of(2024, 1, 1), null);
        this.save("200000", LocalDate.of(2024, 6, 1), null);

        assertEquals(2, this.salaryRepository.findByUserIdOrderByValidFromDesc(this.user.getId()).size());
    }
}
