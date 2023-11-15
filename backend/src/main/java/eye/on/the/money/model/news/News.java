package eye.on.the.money.model.news;

import lombok.*;
import lombok.extern.slf4j.Slf4j;

@Getter
@Setter
@Slf4j
@Builder
@ToString
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
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
