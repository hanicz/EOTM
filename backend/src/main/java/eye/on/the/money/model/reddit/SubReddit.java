package eye.on.the.money.model.reddit;

import eye.on.the.money.util.Generated;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

@Entity
@Getter
@Setter
@Slf4j
@ToString
@Table(name = "EOTM_SUBREDDIT")
@AllArgsConstructor
@NoArgsConstructor
@Generated
public class SubReddit {
    @Id
    private String id;
    private String subreddit;
}
