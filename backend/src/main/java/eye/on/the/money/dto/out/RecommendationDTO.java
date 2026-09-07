package eye.on.the.money.dto.out;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import eye.on.the.money.util.Generated;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@ToString
@Builder
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
@Generated
public class RecommendationDTO {
    private Integer buy;
    private Integer hold;
    @JsonSerialize(using = LocalDateSerializer.class)
    @JsonDeserialize(using = LocalDateDeserializer.class)
    private LocalDate period;
    private Integer sell;
    private Integer strongBuy;
    private Integer strongSell;
    private String symbol;
}
