package eye.on.the.money.dto.out;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import eye.on.the.money.dto.in.EODCandleQuoteDTO;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Getter
@Setter
@ToString
@AllArgsConstructor
@EqualsAndHashCode
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CandleQuoteDTO {
    private Double[] c;
    private Double[] h;
    private Double[] l;
    private Double[] o;
    private Long[] t;
    private Long[] v;

    public static CandleQuoteDTO createFromEODResponse(int size, List<EODCandleQuoteDTO> eodList, JsonNode sameDay) {
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
            t[i] = eodList.get(i).getDate().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        }

        if (sameDay != null) {
            LocalDate date = Instant.ofEpochSecond(sameDay.findValue("timestamp").longValue()).atZone(ZoneId.systemDefault()).toLocalDate();
            c[eodList.size()] = sameDay.findValue("close").doubleValue();
            o[eodList.size()] = sameDay.findValue("open").doubleValue();
            l[eodList.size()] = sameDay.findValue("low").doubleValue();
            h[eodList.size()] = sameDay.findValue("high").doubleValue();
            v[eodList.size()] = sameDay.findValue("volume").longValue();
            t[eodList.size()] = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        }

        return new CandleQuoteDTO(c, h, l, o, t, v);
    }
}
