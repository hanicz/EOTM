package eye.on.the.money.controller;

import eye.on.the.money.dto.out.NoteDTO;
import eye.on.the.money.service.note.NoteService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class NoteControllerTest {

    private static final Long USER_ID = 42L;

    @Mock
    private NoteService noteService;
    @InjectMocks
    private NoteController noteController;

    @Test
    void getNote_returnsTheStoredNote() {
        NoteDTO note = NoteDTO.builder()
                .content("Rebalance in October")
                .updatedAt(LocalDateTime.of(2026, 3, 14, 9, 30))
                .build();
        when(this.noteService.getNote(USER_ID)).thenReturn(note);

        ResponseEntity<NoteDTO> response = this.noteController.getNote(USER_ID);

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertEquals(note, response.getBody());
    }

    @Test
    void updateNote_passesTheNewContentToTheService() {
        NoteDTO request = NoteDTO.builder().content("Check the dividend date").build();
        NoteDTO saved = NoteDTO.builder()
                .content("Check the dividend date")
                .updatedAt(LocalDateTime.of(2026, 3, 14, 10, 0))
                .build();
        when(this.noteService.updateNote(USER_ID, request)).thenReturn(saved);

        ResponseEntity<NoteDTO> response = this.noteController.updateNote(USER_ID, request);

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertEquals(saved, response.getBody());
        verify(this.noteService).updateNote(USER_ID, request);
    }
}
