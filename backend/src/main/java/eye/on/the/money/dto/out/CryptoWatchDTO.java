package eye.on.the.money.dto.out;

import lombok.*;
import lombok.extern.slf4j.Slf4j;

@Getter
@Setter
@Slf4j
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class CryptoWatchDTO implements Comparable<CryptoWatchDTO> {
    private Long cryptoWatchId;
    private String coinId;
    private String symbol;
    private Double liveValue;
    private String name;
    private Double change;
    private String url;

    @Override
    public int compareTo(CryptoWatchDTO cw) {
        if (this.liveValue == null) {
            return -1;
        }
        return this.liveValue.compareTo(cw.getLiveValue());
    }
}
