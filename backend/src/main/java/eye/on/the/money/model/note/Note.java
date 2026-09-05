package eye.on.the.money.model.note;

import com.fasterxml.jackson.annotation.JsonIgnore;
import eye.on.the.money.model.User;
import eye.on.the.money.util.Generated;
import jakarta.persistence.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@Slf4j
@ToString(exclude = "user")
@Table(name = "EOTM_NOTE",
        uniqueConstraints = @UniqueConstraint(name = "UK_EOTM_NOTE_USER", columnNames = "user_id"))
@AllArgsConstructor
@NoArgsConstructor
@Generated
public class Note {

    public static final int MAX_CONTENT_LENGTH = 10000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = MAX_CONTENT_LENGTH)
    private String content;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;
}
