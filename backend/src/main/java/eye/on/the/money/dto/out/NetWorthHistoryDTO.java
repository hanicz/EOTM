package eye.on.the.money.dto.out;

import eye.on.the.money.util.Generated;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@EqualsAndHashCode
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Generated
public class NetWorthHistoryDTO {

    private String currency;

    private List<NetWorthPointDTO> points;

    private List<MonthlyPerformanceDTO> months;
}
