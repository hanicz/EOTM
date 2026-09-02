package eye.on.the.money.dto.out;

import com.fasterxml.jackson.databind.ObjectMapper;
import eye.on.the.money.config.AppConfig;
import eye.on.the.money.dto.Lot;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

class LotWireFormatTest {

    private final ObjectMapper objectMapper = new AppConfig().objectMapper();

    @Test
    void theLotBookkeepingFlagStaysOffTheWire() {
        List<Lot<?>> lots = List.of(
                InvestmentDTO.builder().shortName("CRSR").exchange("US").quantity(0).amount(-100.0).build(),
                ETFInvestmentDTO.builder().shortName("VWCE").exchange("MI").quantity(0).amount(-40.0).build(),
                TransactionDTO.builder().symbol("ADA").quantity(0.0).amount(-1031.24).build(),
                SecurityTransactionDTO.builder().securityId("SEC1").quantity(0).amount(-25.0).build());

        for (Lot<?> lot : lots) {
            String json = Assertions.assertDoesNotThrow(() -> this.objectMapper.writeValueAsString(lot));
            Assertions.assertFalse(json.contains("\"closed\""),
                    lot.getClass().getSimpleName() + " leaked the closed flag: " + json);
        }
    }
}
