package eye.on.the.money.model.stock;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Getter
@Setter
@ToString
@AllArgsConstructor
@EqualsAndHashCode
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CandleQuote {
    private Double[] c;
    private Double[] h;
    private Double[] l;
    private Double[] o;
    private Long[] t;
    private Long[] v;

    public static CandleQuote createFromEODResponse(int size, List<EODCandleQuote> eodList, JsonNode sameDay) {
        Double[] c = new Double[size];
        Double[] o = new Double[size];
        Double[] l = new Double[size];
        Double[] h = new Double[size];
        Long[] v = new Long[size];
        Long[] t = new Long[size];

        for (int i = 0; i < eodList.size(); i++) {
            c[i] = eodList.get(i).getClose();
            o[i] = eodList.get(i).getOpen();
            l[i] = eodList.get(i).getLow();
            h[i] = eodList.get(i).getHigh();
            v[i] = eodList.get(i).getVolume();
            t[i] = eodList.get(i).getDate().getTime();
        }

        if(sameDay != null) {
            c[eodList.size()] = sameDay.findValue("close").doubleValue();
            o[eodList.size()] = sameDay.findValue("open").doubleValue();
            l[eodList.size()] = sameDay.findValue("low").doubleValue();
            h[eodList.size()] = sameDay.findValue("high").doubleValue();
            v[eodList.size()] = sameDay.findValue("volume").longValue();
            t[eodList.size()] = TimeUnit.SECONDS.toMillis(sameDay.findValue("timestamp").longValue());
        }

        return new CandleQuote(c, h, l, o, t, v);
    }
}
