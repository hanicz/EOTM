package eye.on.the.money.repository.financial;

import eye.on.the.money.EotmApplication;
import eye.on.the.money.dto.out.MonthlyCashFlowDTO;
import eye.on.the.money.dto.out.MonthlyIncomeDTO;
import eye.on.the.money.model.Currency;
import eye.on.the.money.model.User;
import eye.on.the.money.model.financial.BankTransaction;
import eye.on.the.money.model.financial.BankTransactionTax;
import eye.on.the.money.model.financial.TaxDetails;
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
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = EotmApplication.class)
@ActiveProfiles("test")
@Transactional
class BankTransactionRepositoryTest {

    private static final String USER_EMAIL = "test@test.test";

    @Autowired
    private BankTransactionRepository bankTransactionRepository;

    @Autowired
    private BankTransactionTaxRepository bankTransactionTaxRepository;

    @Autowired
    private CurrencyRepository currencyRepository;

    @Autowired
    private UserService userService;

    @PersistenceContext
    private EntityManager entityManager;

    private User user;
    private Currency huf;
    private Currency eur;

    @BeforeEach
    void setUp() {
        this.bankTransactionRepository.deleteAll();
        this.user = this.userService.loadUserByEmail(USER_EMAIL);
        this.huf = this.currencyRepository.findById("HUF").orElseThrow();
        this.eur = this.currencyRepository.findById("EUR").orElseThrow();
    }

    private BankTransaction persistTaxable(LocalDate bookingDate, Currency currency, double amount) {
        BankTransaction transaction = this.persist(bookingDate, currency, amount, false);
        transaction.setTaxable(true);
        this.bankTransactionRepository.save(transaction);
        this.bankTransactionTaxRepository.save(BankTransactionTax.builder()
                .bankTransaction(transaction)
                .taxDetails(TaxDetails.builder()
                        .rate(BigDecimal.ONE)
                        .rateDate(bookingDate)
                        .amountInHuf(BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP))
                        .taxBase(new BigDecimal("890000.00"))
                        .szocho(new BigDecimal("115700"))
                        .szja(new BigDecimal("133500"))
                        .total(new BigDecimal("249200"))
                        .calculatedOn(LocalDate.of(2026, 1, 1))
                        .build())
                .build());
        this.entityManager.flush();
        this.entityManager.clear();
        return transaction;
    }

    private BankTransaction persist(LocalDate bookingDate, Currency currency, double amount, boolean excluded) {
        BankTransaction transaction = this.bankTransactionRepository.save(BankTransaction.builder()
                .bankTransactionId("TX" + amount + bookingDate)
                .bookingDate(bookingDate)
                .type("Utalas")
                .memo("memo")
                .amount(amount)
                .excluded(excluded)
                .creationDate(LocalDate.now())
                .currency(currency)
                .user(this.user)
                .build());
        this.entityManager.flush();
        return transaction;
    }

    @Test
    void findMonthlyCashFlow_splitsInAndOutPerMonth() {
        this.persist(LocalDate.of(2025, 12, 1), this.huf, 850000.0, false);
        this.persist(LocalDate.of(2025, 12, 15), this.huf, -275.0, false);
        this.persist(LocalDate.of(2025, 12, 20), this.huf, -1000.0, false);
        this.persist(LocalDate.of(2025, 11, 5), this.huf, -500.0, false);

        List<MonthlyCashFlowDTO> result = this.bankTransactionRepository.findMonthlyCashFlow(this.user.getId());

        assertEquals(2, result.size());
        MonthlyCashFlowDTO december = result.getFirst();
        assertEquals(2025, december.getYear());
        assertEquals(12, december.getMonth());
        assertEquals("HUF", december.getCurrencyId());
        assertEquals(850000.0, december.getMoneyIn());
        assertEquals(-1275.0, december.getMoneyOut());
        assertEquals(848725.0, december.getNet());

        MonthlyCashFlowDTO november = result.get(1);
        assertEquals(11, november.getMonth());
        assertEquals(0.0, november.getMoneyIn());
        assertEquals(-500.0, november.getMoneyOut());
    }

    @Test
    void findMonthlyCashFlow_reportsWhatShareOfIncomeWasKept() {
        this.persist(LocalDate.of(2025, 12, 1), this.huf, 1000000.0, false);
        this.persist(LocalDate.of(2025, 12, 5), this.huf, -400000.0, false);

        MonthlyCashFlowDTO december = this.bankTransactionRepository.findMonthlyCashFlow(this.user.getId()).getFirst();

        assertEquals(600000.0, december.getNet());
        assertEquals(60.0, december.getSavedPercent());
    }

    @Test
    void findMonthlyCashFlow_reportsNegativePercentWhenSpendingBeatsIncome() {
        this.persist(LocalDate.of(2025, 12, 1), this.huf, 100000.0, false);
        this.persist(LocalDate.of(2025, 12, 5), this.huf, -150000.0, false);

        MonthlyCashFlowDTO december = this.bankTransactionRepository.findMonthlyCashFlow(this.user.getId()).getFirst();

        assertEquals(-50.0, december.getSavedPercent());
    }

    @Test
    void findMonthlyCashFlow_leavesPercentBlankWhenNothingCameIn() {
        this.persist(LocalDate.of(2025, 12, 5), this.huf, -150000.0, false);

        MonthlyCashFlowDTO december = this.bankTransactionRepository.findMonthlyCashFlow(this.user.getId()).getFirst();

        assertEquals(0.0, december.getMoneyIn());
        assertNull(december.getSavedPercent());
    }

    @Test
    void findMonthlyCashFlow_keepsCurrenciesApart() {
        this.persist(LocalDate.of(2025, 12, 1), this.huf, 850000.0, false);
        this.persist(LocalDate.of(2025, 12, 2), this.eur, 100.0, false);

        List<MonthlyCashFlowDTO> result = this.bankTransactionRepository.findMonthlyCashFlow(this.user.getId());

        assertEquals(2, result.size());
        assertEquals("EUR", result.getFirst().getCurrencyId());
        assertEquals(100.0, result.getFirst().getMoneyIn());
        assertEquals("HUF", result.get(1).getCurrencyId());
        assertEquals(850000.0, result.get(1).getMoneyIn());
    }

    @Test
    void findMonthlyCashFlow_leavesOutExcludedRecords() {
        this.persist(LocalDate.of(2025, 12, 1), this.huf, 850000.0, false);
        this.persist(LocalDate.of(2025, 12, 2), this.huf, -300000.0, true);

        List<MonthlyCashFlowDTO> result = this.bankTransactionRepository.findMonthlyCashFlow(this.user.getId());

        assertEquals(1, result.size());
        assertEquals(850000.0, result.getFirst().getMoneyIn());
        assertEquals(0.0, result.getFirst().getMoneyOut());
    }

    @Test
    void findMonthlyCashFlow_emptyWhenEverythingIsExcluded() {
        this.persist(LocalDate.of(2025, 12, 1), this.huf, 850000.0, true);

        assertTrue(this.bankTransactionRepository.findMonthlyCashFlow(this.user.getId()).isEmpty());
    }

    private BankTransaction persistIncome(LocalDate bookingDate, double amount, String type, String partnerName) {
        BankTransaction transaction = this.bankTransactionRepository.save(BankTransaction.builder()
                .bankTransactionId("TX" + amount + bookingDate + partnerName)
                .bookingDate(bookingDate)
                .type(type)
                .partnerName(partnerName)
                .memo("memo")
                .amount(amount)
                .excluded(false)
                .creationDate(LocalDate.now())
                .currency(this.huf)
                .user(this.user)
                .build());
        this.entityManager.flush();
        return transaction;
    }

    @Test
    void findMonthlyIncome_groupsBySourceAndRanksByAmount() {
        this.persistIncome(LocalDate.of(2025, 12, 5), 850000.0, "Jovairas", "MUNKAADO ZRT");
        this.persistIncome(LocalDate.of(2025, 12, 20), 150000.0, "Jovairas", "MUNKAADO ZRT");
        this.persistIncome(LocalDate.of(2025, 12, 9), 30000.0, "Jovairas", "MASIK KFT");

        List<MonthlyIncomeDTO> result = this.bankTransactionRepository.findMonthlyIncome(this.user.getId());

        assertEquals(2, result.size());
        assertEquals("MUNKAADO ZRT", result.getFirst().getSource());
        assertEquals(1000000.0, result.getFirst().getAmount());
        assertEquals(2L, result.getFirst().getTransactionCount());
        assertEquals("MASIK KFT", result.get(1).getSource());
        assertEquals(30000.0, result.get(1).getAmount());
        assertEquals(1L, result.get(1).getTransactionCount());
    }

    @Test
    void findMonthlyIncome_fallsBackToTypeWhenPartnerIsBlank() {
        this.persistIncome(LocalDate.of(2025, 12, 5), 250.75, "Deviza jovairas", "");
        this.persistIncome(LocalDate.of(2025, 12, 6), 100.0, "Kamat", "   ");

        List<MonthlyIncomeDTO> result = this.bankTransactionRepository.findMonthlyIncome(this.user.getId());

        assertEquals(2, result.size());
        assertEquals("Deviza jovairas", result.getFirst().getSource());
        assertEquals("Kamat", result.get(1).getSource());
    }

    @Test
    void findMonthlyIncome_leavesOutSpendingAndExcludedRecords() {
        this.persistIncome(LocalDate.of(2025, 12, 5), 850000.0, "Jovairas", "MUNKAADO ZRT");
        this.persist(LocalDate.of(2025, 12, 6), this.huf, -275.0, false);
        this.persist(LocalDate.of(2025, 12, 7), this.huf, 500000.0, true);

        List<MonthlyIncomeDTO> result = this.bankTransactionRepository.findMonthlyIncome(this.user.getId());

        assertEquals(1, result.size());
        assertEquals(850000.0, result.getFirst().getAmount());
    }

    @Test
    void findMonthlyIncome_separatesMonthsAndCurrencies() {
        this.persistIncome(LocalDate.of(2025, 12, 5), 850000.0, "Jovairas", "MUNKAADO ZRT");
        this.persistIncome(LocalDate.of(2025, 11, 5), 800000.0, "Jovairas", "MUNKAADO ZRT");
        this.persist(LocalDate.of(2025, 12, 8), this.eur, 250.75, false);

        List<MonthlyIncomeDTO> result = this.bankTransactionRepository.findMonthlyIncome(this.user.getId());

        assertEquals(3, result.size());
        assertEquals(12, result.getFirst().getMonth());
        assertEquals("EUR", result.getFirst().getCurrencyId());
        assertEquals(12, result.get(1).getMonth());
        assertEquals("HUF", result.get(1).getCurrencyId());
        assertEquals(11, result.get(2).getMonth());
        assertEquals(800000.0, result.get(2).getAmount());
    }

    @Test
    void updateExcluded_flagsOnlyTheGivenRecords() {
        BankTransaction first = this.persist(LocalDate.of(2025, 12, 1), this.huf, 100.0, false);
        BankTransaction second = this.persist(LocalDate.of(2025, 12, 2), this.huf, 200.0, false);

        int updated = this.bankTransactionRepository
                .updateExcludedByUserIdAndIdIn(this.user.getId(), List.of(first.getId()), true);

        assertEquals(1, updated);
        assertTrue(this.bankTransactionRepository.findById(first.getId()).orElseThrow().isExcluded());
        assertFalse(this.bankTransactionRepository.findById(second.getId()).orElseThrow().isExcluded());
    }

    @Test
    void updateExcluded_ignoresRecordsOfAnotherUser() {
        BankTransaction transaction = this.persist(LocalDate.of(2025, 12, 1), this.huf, 100.0, false);

        int updated = this.bankTransactionRepository
                .updateExcludedByUserIdAndIdIn(-1L, List.of(transaction.getId()), true);

        assertEquals(0, updated);
        assertFalse(this.bankTransactionRepository.findById(transaction.getId()).orElseThrow().isExcluded());
    }

    @Test
    void findTaxable_returnsOnlyFlaggedRecordsNewestFirst() {
        BankTransaction older = this.persistTaxable(LocalDate.of(2025, 11, 1), this.huf, 100.0);
        BankTransaction newer = this.persistTaxable(LocalDate.of(2025, 12, 1), this.eur, 200.0);
        this.persist(LocalDate.of(2025, 12, 5), this.huf, 300.0, false);

        List<BankTransaction> result =
                this.bankTransactionRepository.findByUserIdAndTaxableTrueOrderByBookingDateDesc(this.user.getId());

        assertEquals(2, result.size());
        assertEquals(newer.getId(), result.getFirst().getId());
        assertEquals(older.getId(), result.get(1).getId());
    }

    @Test
    void findTaxable_readsBackTheStoredTax() {
        this.persistTaxable(LocalDate.of(2025, 12, 1), this.huf, 1000000.0);

        TaxDetails stored = this.bankTransactionTaxRepository.findTaxableByUserId(this.user.getId())
                .getFirst().getTaxDetails();

        assertEquals(0, new BigDecimal("1000000.00").compareTo(stored.getAmountInHuf()));
        assertEquals(0, new BigDecimal("249200").compareTo(stored.getTotal()));
        assertEquals(LocalDate.of(2026, 1, 1), stored.getCalculatedOn());
    }

    @Test
    void deletingATransactionRemovesItsTaxRow() {
        BankTransaction transaction = this.persistTaxable(LocalDate.of(2025, 12, 1), this.huf, 1000000.0);

        this.bankTransactionRepository.deleteByUserIdAndIdIn(this.user.getId(), List.of(transaction.getId()));
        this.entityManager.flush();
        this.entityManager.clear();

        assertTrue(this.bankTransactionTaxRepository.findTaxableByUserId(this.user.getId()).isEmpty());
    }

    @Test
    void findByIds_onlyReturnsTheRecordsOfTheGivenUser() {
        BankTransaction transaction = this.persist(LocalDate.of(2025, 12, 1), this.huf, 100.0, false);

        assertEquals(1, this.bankTransactionRepository
                .findByUserIdAndIdIn(this.user.getId(), List.of(transaction.getId())).size());
        assertTrue(this.bankTransactionRepository
                .findByUserIdAndIdIn(-1L, List.of(transaction.getId())).isEmpty());
    }
}
