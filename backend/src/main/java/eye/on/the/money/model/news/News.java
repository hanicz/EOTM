package eye.on.the.money.model.news;

import lombok.*;
import lombok.extern.slf4j.Slf4j;

@Getter
@Setter
@Slf4j
@ToString
@AllArgsConstructor
@NoArgsConstructor
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
