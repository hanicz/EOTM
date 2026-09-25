package eye.on.the.money.dto.out;

import com.fasterxml.jackson.databind.ObjectMapper;
import eye.on.the.money.config.AppConfig;
import eye.on.the.money.dto.Lot;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

class LotWireFormatTest {

    private final ObjectMapper objectMapper = new AppConfig().objectMapper();

    @Test
    void theLotBookkeepingFlagStaysOffTheWire() {
        List<Lot<?>> lots = List.of(
                InvestmentDTO.builder().shortName("CRSR").exchange("US").quantity(BigDecimal.ZERO).amount(new BigDecimal("-100")).build(),
                ETFInvestmentDTO.builder().shortName("VWCE").exchange("MI").quantity(BigDecimal.ZERO).amount(new BigDecimal("-40")).build(),
                TransactionDTO.builder().symbol("ADA").quantity(BigDecimal.ZERO).amount(new BigDecimal("-1031.24")).build(),
                SecurityTransactionDTO.builder().securityId("SEC1").quantity(0).amount(new BigDecimal("-25")).build(),
                ForexTransactionDTO.builder().fromCurrencyId("HUF").toCurrencyId("EUR")
                        .toAmount(BigDecimal.ZERO).fromAmount(new BigDecimal("-20000")).build());

        for (Lot<?> lot : lots) {
            String json = Assertions.assertDoesNotThrow(() -> this.objectMapper.writeValueAsString(lot));
            Assertions.assertFalse(json.contains("\"closed\""),
                    lot.getClass().getSimpleName() + " leaked the closed flag: " + json);
        }
    }
}
