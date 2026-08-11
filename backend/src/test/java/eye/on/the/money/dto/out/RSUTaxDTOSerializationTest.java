package eye.on.the.money.dto.out;

import com.fasterxml.jackson.databind.ObjectMapper;
import eye.on.the.money.config.AppConfig;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertTrue;

class RSUTaxDTOSerializationTest {

    private final ObjectMapper objectMapper = new AppConfig().objectMapper();

    @Test
    void serializesDatesAsIsoStrings() throws Exception {
        RSUTaxDTO dto = RSUTaxDTO.builder()
                .shortName("AAPL")
                .date(LocalDate.of(2026, 8, 4))
                .priceDate(LocalDate.of(2026, 8, 4))
                .rateDate(LocalDate.of(2026, 8, 4))
                .amountInHuf(new BigDecimal("1000"))
                .build();

        String json = this.objectMapper.writeValueAsString(dto);

        assertTrue(json.contains("\"date\":\"2026-08-04\""), "date was not an ISO string: " + json);
        assertTrue(json.contains("\"priceDate\":\"2026-08-04\""), "priceDate was not an ISO string: " + json);
        assertTrue(json.contains("\"rateDate\":\"2026-08-04\""), "rateDate was not an ISO string: " + json);
    }
}
