package eye.on.the.money.repository;

import eye.on.the.money.EotmApplication;
import eye.on.the.money.model.Currency;
import eye.on.the.money.model.User;
import eye.on.the.money.model.financial.BankTransaction;
import eye.on.the.money.model.note.Note;
import eye.on.the.money.repository.financial.BankTransactionRepository;
import eye.on.the.money.repository.forex.CurrencyRepository;
import eye.on.the.money.repository.note.NoteRepository;
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
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = EotmApplication.class)
@ActiveProfiles("test")
@Transactional
class AuditedEntityTest {

    private static final String USER_EMAIL = "test@test.test";
    private static final LocalDateTime LONG_AGO = LocalDateTime.of(2020, 1, 1, 8, 0);

    @Autowired
    private NoteRepository noteRepository;

    @Autowired
    private BankTransactionRepository bankTransactionRepository;

    @Autowired
    private CurrencyRepository currencyRepository;

    @Autowired
    private UserService userService;

    @PersistenceContext
    private EntityManager entityManager;

    private User user;

    @BeforeEach
    void setUp() {
        this.noteRepository.deleteAll();
        this.bankTransactionRepository.deleteAll();
        this.user = this.userService.loadUserByEmail(USER_EMAIL);
    }

    @Test
    void save_fillsCreatedAtAndUpdatedAt() {
        Note note = this.noteRepository.saveAndFlush(Note.builder().content("Buy more index funds").user(this.user).build());
        this.entityManager.clear();

        Note stored = this.noteRepository.findById(note.getId()).orElseThrow();

        assertNotNull(stored.getCreatedAt());
        assertNotNull(stored.getUpdatedAt());
    }

    @Test
    void update_advancesUpdatedAtButKeepsCreatedAt() {
        Note note = this.noteRepository.saveAndFlush(Note.builder().content("First draft").user(this.user).build());
        this.backdate("Note", note.getId());
        LocalDateTime createdAt = this.noteRepository.findById(note.getId()).orElseThrow().getCreatedAt();

        Note stored = this.noteRepository.findById(note.getId()).orElseThrow();
        stored.setContent("Second draft");
        stored.setCreatedAt(LocalDateTime.now());
        this.noteRepository.saveAndFlush(stored);
        this.entityManager.clear();

        Note reloaded = this.noteRepository.findById(note.getId()).orElseThrow();
        assertEquals(createdAt, reloaded.getCreatedAt());
        assertTrue(reloaded.getUpdatedAt().isAfter(LONG_AGO));
    }

    @Test
    void bulkUpdate_advancesUpdatedAt() {
        Currency huf = this.currencyRepository.findById("HUF").orElseThrow();
        BankTransaction transaction = this.bankTransactionRepository.saveAndFlush(BankTransaction.builder()
                .bankTransactionId("TX-AUDIT-1")
                .bookingDate(LocalDate.of(2026, 2, 3))
                .type("Utalas")
                .memo("memo")
                .amount(-1200.0)
                .excluded(false)
                .currency(huf)
                .user(this.user)
                .build());
        this.backdate("BankTransaction", transaction.getId());

        this.bankTransactionRepository.updateExcludedByUserIdAndIdIn(this.user.getId(), List.of(transaction.getId()), true);

        BankTransaction reloaded = this.bankTransactionRepository.findById(transaction.getId()).orElseThrow();
        assertTrue(reloaded.isExcluded());
        assertTrue(reloaded.getUpdatedAt().isAfter(LONG_AGO));
    }

    private void backdate(String entity, Long id) {
        this.entityManager.createQuery("UPDATE " + entity + " e SET e.createdAt = :old, e.updatedAt = :old WHERE e.id = :id")
                .setParameter("old", LONG_AGO)
                .setParameter("id", id)
                .executeUpdate();
        this.entityManager.clear();
    }
}
