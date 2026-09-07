package eye.on.the.money.model.news;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import eye.on.the.money.util.Generated;
import lombok.*;

@Getter
@Setter
@Builder
@ToString
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@Generated
@JsonIgnoreProperties(ignoreUnknown = true)
public class News {
    private Long id;
    private String category;
    private Long datetime;
    private String headline;
    private String image;
    private String source;
    private String summary;
    private String url;
}
