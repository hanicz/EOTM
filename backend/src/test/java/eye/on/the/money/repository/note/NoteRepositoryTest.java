package eye.on.the.money.repository.note;

import eye.on.the.money.EotmApplication;
import eye.on.the.money.model.User;
import eye.on.the.money.model.note.Note;
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

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = EotmApplication.class)
@ActiveProfiles("test")
@Transactional
class NoteRepositoryTest {

    private static final String USER_EMAIL = "test@test.test";

    @Autowired
    private NoteRepository noteRepository;

    @Autowired
    private UserService userService;

    @PersistenceContext
    private EntityManager entityManager;

    private User user;

    @BeforeEach
    void setUp() {
        this.noteRepository.deleteAll();
        this.user = this.userService.loadUserByEmail(USER_EMAIL);
    }

    @Test
    void findByUserId_returnsTheStoredNote() {
        this.noteRepository.saveAndFlush(Note.builder()
                .content("Rebalance in October")
                .updatedAt(LocalDateTime.of(2026, 3, 14, 9, 30))
                .user(this.user)
                .build());
        this.entityManager.clear();

        Optional<Note> found = this.noteRepository.findByUserId(this.user.getId());

        assertTrue(found.isPresent());
        assertEquals("Rebalance in October", found.get().getContent());
    }

    @Test
    void findByUserId_isEmptyForAUserWithoutANote() {
        assertTrue(this.noteRepository.findByUserId(this.user.getId()).isEmpty());
    }

    @Test
    void content_holdsALongNote() {
        String longNote = "x".repeat(Note.MAX_CONTENT_LENGTH);
        this.noteRepository.saveAndFlush(Note.builder()
                .content(longNote)
                .updatedAt(LocalDateTime.now())
                .user(this.user)
                .build());
        this.entityManager.clear();

        assertEquals(longNote, this.noteRepository.findByUserId(this.user.getId()).orElseThrow().getContent());
    }

    @Test
    void aUserCannotHaveTwoNotes() {
        this.noteRepository.saveAndFlush(Note.builder()
                .content("First")
                .updatedAt(LocalDateTime.now())
                .user(this.user)
                .build());

        assertThrows(DataIntegrityViolationException.class, () ->
                this.noteRepository.saveAndFlush(Note.builder()
                        .content("Second")
                        .updatedAt(LocalDateTime.now())
                        .user(this.user)
                        .build()));
    }
}
