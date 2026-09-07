package eye.on.the.money.service.note;

import eye.on.the.money.dto.out.NoteDTO;
import eye.on.the.money.model.User;
import eye.on.the.money.model.note.Note;
import eye.on.the.money.repository.note.NoteRepository;
import eye.on.the.money.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class NoteService {

    private final NoteRepository noteRepository;
    private final UserService userService;

    public NoteDTO getNote(Long userId) {
        return this.noteRepository.findByUserId(userId)
                .map(note -> this.toDTO(note.getContent(), note.getUpdatedAt()))
                .orElseGet(() -> this.toDTO("", null));
    }

    @Transactional
    public NoteDTO updateNote(Long userId, NoteDTO noteDTO) {
        Note note = this.noteRepository.findByUserId(userId).orElseGet(() -> {
            User user = this.userService.getReference(userId);
            return Note.builder().user(user).build();
        });
        note.setContent(noteDTO.getContent() == null ? "" : noteDTO.getContent());
        note.setUpdatedAt(LocalDateTime.now());
        note = this.noteRepository.save(note);
        return this.toDTO(note.getContent(), note.getUpdatedAt());
    }

    private NoteDTO toDTO(String content, LocalDateTime updatedAt) {
        return NoteDTO.builder().content(content).updatedAt(updatedAt).build();
    }
}
