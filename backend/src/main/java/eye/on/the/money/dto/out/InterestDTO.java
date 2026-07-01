package eye.on.the.money.dto.out;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import eye.on.the.money.dto.CSVHelper;
import eye.on.the.money.util.Generated;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVRecord;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Getter
@Setter
@Builder
@Slf4j
@ToString
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@Generated
public class InterestDTO implements CSVHelper {
    private Long interestId;
    private Double amount;
    @JsonSerialize(using = LocalDateSerializer.class)
    @JsonDeserialize(using = LocalDateDeserializer.class)
    private LocalDate interestDate;
    private String securityId;
    private String securityName;
    private String currencyId;

    @Override
    @JsonIgnore
    public Object[] getHeaders() {
        return new String[]{"Interest Id", "Amount", "Interest Date", "Security Id", "Security Name", "Currency"};
    }

    @Override
    @JsonIgnore
    public Object[] getCSVRecord() {
        return new Object[]{this.getInterestId(), this.getAmount(),
                this.getInterestDate(), this.getSecurityId(), this.getSecurityName(), this.getCurrencyId()};
    }

    public static InterestDTO createFromCSVRecord(CSVRecord csvRecord, DateTimeFormatter formatter) {
        return InterestDTO.builder()
                .interestId(csvRecord.get("Interest Id").isBlank() ? null : Long.parseLong(csvRecord.get("Interest Id")))
                .interestDate(LocalDate.parse(csvRecord.get("Interest Date"), formatter))
                .amount(Double.parseDouble(csvRecord.get("Amount")))
                .currencyId(csvRecord.get("Currency"))
                .securityId(csvRecord.get("Security Id"))
                .securityName(csvRecord.get("Security Name"))
                .build();
    }
}
