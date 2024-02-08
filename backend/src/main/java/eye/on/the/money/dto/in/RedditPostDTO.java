package eye.on.the.money.dto.in;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import eye.on.the.money.util.Generated;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@Setter
@Builder
@EqualsAndHashCode
@ToString
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Generated
public class RedditPostDTO {
    private String title;
    private String selftext;
    private String thumbnail;
    private boolean stickied;
    private String permalink;
}
