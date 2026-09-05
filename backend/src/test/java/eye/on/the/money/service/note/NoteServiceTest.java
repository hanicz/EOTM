package eye.on.the.money.service.note;

import eye.on.the.money.dto.out.NoteDTO;
import eye.on.the.money.model.User;
import eye.on.the.money.model.note.Note;
import eye.on.the.money.repository.note.NoteRepository;
import eye.on.the.money.service.user.UserService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class NoteServiceTest {

    private static final Long USER_ID = 42L;

    @Mock
    private NoteRepository noteRepository;
    @Mock
    private UserService userService;
    @InjectMocks
    private NoteService noteService;

    private final User user = User.builder().id(USER_ID).email("note@example.com").build();

    @Test
    void getNote_returnsAnEmptyNoteWhenNoRowExists() {
        when(this.noteRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        NoteDTO result = this.noteService.getNote(USER_ID);

        Assertions.assertEquals("", result.getContent());
        Assertions.assertNull(result.getUpdatedAt());
    }

    @Test
    void getNote_returnsStoredContentAndTimestamp() {
        LocalDateTime savedAt = LocalDateTime.of(2026, 3, 14, 9, 30);
        when(this.noteRepository.findByUserId(USER_ID)).thenReturn(Optional.of(
                Note.builder().id(1L).content("Rebalance in October").updatedAt(savedAt).user(this.user).build()));

        NoteDTO result = this.noteService.getNote(USER_ID);

        Assertions.assertEquals("Rebalance in October", result.getContent());
        Assertions.assertEquals(savedAt, result.getUpdatedAt());
    }

    @Test
    void updateNote_createsARowForAUserWithoutOne() {
        when(this.noteRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
        when(this.userService.getReference(USER_ID)).thenReturn(this.user);
        when(this.noteRepository.save(any(Note.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NoteDTO result = this.noteService.updateNote(USER_ID,
                NoteDTO.builder().content("Check the dividend date").build());

        ArgumentCaptor<Note> captor = ArgumentCaptor.forClass(Note.class);
        verify(this.noteRepository).save(captor.capture());
        Assertions.assertEquals(this.user, captor.getValue().getUser());
        Assertions.assertEquals("Check the dividend date", captor.getValue().getContent());
        Assertions.assertNotNull(captor.getValue().getUpdatedAt());
        Assertions.assertEquals("Check the dividend date", result.getContent());
        Assertions.assertNotNull(result.getUpdatedAt());
    }

    @Test
    void updateNote_overwritesTheExistingRowAndRestampsIt() {
        LocalDateTime savedAt = LocalDateTime.of(2026, 3, 14, 9, 30);
        Note existing = Note.builder().id(1L).content("Old note").updatedAt(savedAt).user(this.user).build();
        when(this.noteRepository.findByUserId(USER_ID)).thenReturn(Optional.of(existing));
        when(this.noteRepository.save(existing)).thenReturn(existing);

        NoteDTO result = this.noteService.updateNote(USER_ID, NoteDTO.builder().content("New note").build());

        verify(this.userService, never()).getReference(any());
        Assertions.assertEquals("New note", result.getContent());
        Assertions.assertTrue(result.getUpdatedAt().isAfter(savedAt));
    }

    @Test
    void updateNote_storesAnEmptyStringWhenTheContentIsMissing() {
        when(this.noteRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
        when(this.userService.getReference(USER_ID)).thenReturn(this.user);
        when(this.noteRepository.save(any(Note.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NoteDTO result = this.noteService.updateNote(USER_ID, NoteDTO.builder().build());

        Assertions.assertEquals("", result.getContent());
    }
}
