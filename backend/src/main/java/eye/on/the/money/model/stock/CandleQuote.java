package eye.on.the.money.model.stock;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CandleQuote {
    private Double[] c;
    private Double[] h;
    private Double[] l;
    private Double[] o;
    private Long[] t;
    private Long[] v;

    public boolean sameSize() {
        return (c.length == h.length && c.length == l.length && c.length == o.length && c.length == t.length && c.length == v.length);
    }
}
