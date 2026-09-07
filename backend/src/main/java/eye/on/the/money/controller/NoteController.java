package eye.on.the.money.controller;

import eye.on.the.money.dto.out.NoteDTO;
import eye.on.the.money.security.CurrentUserId;
import eye.on.the.money.service.note.NoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/note")
@Validated
@RequiredArgsConstructor
public class NoteController {

    private final NoteService noteService;

    @GetMapping
    public ResponseEntity<NoteDTO> getNote(@CurrentUserId Long userId) {
        return ResponseEntity.ok(this.noteService.getNote(userId));
    }

    @PutMapping
    public ResponseEntity<NoteDTO> updateNote(@CurrentUserId Long userId, @RequestBody @Valid NoteDTO noteDTO) {
        return ResponseEntity.ok(this.noteService.updateNote(userId, noteDTO));
    }
}
