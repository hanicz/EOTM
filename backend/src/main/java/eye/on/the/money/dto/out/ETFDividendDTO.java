package eye.on.the.money.dto.out;

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
public class ETFDividendDTO implements CSVHelper {
    private Long id;
    private Double amount;
    @JsonSerialize(using = LocalDateSerializer.class)
    @JsonDeserialize(using = LocalDateDeserializer.class)
    private LocalDate dividendDate;
    private String shortName;
    private String currencyId;
    private String exchange;

    @Override
    public Object[] getHeaders() {
        return new String[]{"Dividend Id", "Amount", "Dividend Date", "Short Name", "Currency"};
    }

    @Override
    public Object[] getCSVRecord() {
        return new Object[]{this.getId(), this.getAmount(),
                this.getDividendDate(), this.getShortName(),
                this.getCurrencyId()};
    }

    public static ETFDividendDTO createFromCSVRecord(CSVRecord csvRecord, DateTimeFormatter formatter) {
        return ETFDividendDTO.builder()
                .id(csvRecord.get("Dividend Id").isBlank() ? null : Long.parseLong(csvRecord.get("Dividend Id")))
                .dividendDate(LocalDate.parse(csvRecord.get("Dividend Date"), formatter))
                .amount(Double.parseDouble(csvRecord.get("Amount")))
                .currencyId(csvRecord.get("Currency"))
                .shortName(csvRecord.get("Short Name"))
                .build();
    }
}
