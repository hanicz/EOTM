package eye.on.the.money.dto.out;

import com.fasterxml.jackson.annotation.JsonFormat;
import eye.on.the.money.util.Generated;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalTime;
import java.util.List;

@Getter
@Setter
@Builder
@Slf4j
@ToString
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@Generated
public class MarketExchangeDTO {
    private String code;
    private String name;
    private String timeZone;
    private String currency;
    private String countryISO2;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
    private LocalTime openTime;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
    private LocalTime closeTime;
    private List<MarketHolidayDTO> holidays;
}
