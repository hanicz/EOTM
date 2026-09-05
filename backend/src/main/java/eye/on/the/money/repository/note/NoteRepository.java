package eye.on.the.money.repository.note;

import eye.on.the.money.model.note.Note;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NoteRepository extends JpaRepository<Note, Long> {

    Optional<Note> findByUserId(Long userId);
}
