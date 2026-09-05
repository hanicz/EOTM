package eye.on.the.money.dto.out;

import eye.on.the.money.model.note.Note;
import eye.on.the.money.util.Generated;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

@Slf4j
@Getter
@Setter
@Builder
@EqualsAndHashCode
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Generated
public class NoteDTO {

    @Size(max = Note.MAX_CONTENT_LENGTH)
    private String content;

    private LocalDateTime updatedAt;
}
