package eye.on.the.money.dto.out;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ActiveProfiles("test")
class ETFInvestmentDTOTest {

    @Test
    public void mergeInvestments() {
        ETFInvestmentDTO eiDTO1 = this.getBaseDTO();
        ETFInvestmentDTO eiDTO2 = ETFInvestmentDTO.builder()
                .amount(15.0)
                .quantity(667)
                .id(2L)
                .buySell("B")
                .liveValue(55.4)
                .shortName("AMD")
                .valueDiff(77.8)
                .build();
        ETFInvestmentDTO baseDTO = this.getBaseDTO();

        eiDTO1.mergeInvestments(eiDTO2);

        Assertions.assertAll("Assert all changing values",
                () -> assertEquals(eiDTO2.getAmount() + baseDTO.getAmount(), eiDTO1.getAmount()),
                () -> assertEquals(eiDTO2.getQuantity() + baseDTO.getQuantity(), eiDTO1.getQuantity()),
                () -> assertEquals("B", eiDTO1.getBuySell()));
    }

    @Test
    public void mergeInvestmentsWithSell() {
        ETFInvestmentDTO eiDTO1 = this.getBaseDTO();
        eiDTO1.setBuySell("S");
        ETFInvestmentDTO eiDTO2 = ETFInvestmentDTO.builder()
                .amount(10.0)
                .quantity(6)
                .id(2L)
                .buySell("B")
                .liveValue(22.7)
                .shortName("AMD")
                .valueDiff(55.3)
                .build();
        ETFInvestmentDTO baseDTO = this.getBaseDTO();

        eiDTO1.mergeInvestments(eiDTO2);

        Assertions.assertAll("Assert all changing values",
                () -> assertEquals(eiDTO2.getAmount() + baseDTO.getAmount(), eiDTO1.getAmount()),
                () -> assertEquals(eiDTO2.getQuantity() + baseDTO.getQuantity(), eiDTO1.getQuantity()),
                () -> assertEquals("B", eiDTO1.getBuySell()));
    }

    @Test
    public void mergeInvestmentsDifferentShortName() {
        ETFInvestmentDTO eiDTO1 = this.getBaseDTO();
        ETFInvestmentDTO eiDTO2 = ETFInvestmentDTO.builder()
                .amount(15.0)
                .quantity(667)
                .id(2L)
                .buySell("B")
                .liveValue(55.4)
                .shortName("CRSR")
                .valueDiff(77.8)
                .build();

        eiDTO1.mergeInvestments(eiDTO2);

        Assertions.assertAll("Assert all changing values",
                () -> assertEquals(15.0, eiDTO1.getAmount()),
                () -> assertEquals(667, eiDTO1.getQuantity()),
                () -> assertEquals(55.4, eiDTO1.getLiveValue()),
                () -> assertEquals(77.8, eiDTO1.getValueDiff()),
                () -> assertEquals("B", eiDTO1.getBuySell()));
    }

    @Test
    public void negateAmountAndQuantity() {
        ETFInvestmentDTO eiDTO = ETFInvestmentDTO.builder().amount(15.0).quantity(667).build();
        eiDTO.negateAmountAndQuantity();

        Assertions.assertAll("Assert all negated values",
                () -> assertEquals(-15.0, eiDTO.getAmount()),
                () -> assertEquals(-667, eiDTO.getQuantity()));
    }

    @Test
    public void negateAmountAndZero() {
        ETFInvestmentDTO eiDTO = ETFInvestmentDTO.builder().amount(0.0).quantity(0).build();
        eiDTO.negateAmountAndQuantity();

        Assertions.assertAll("Assert all negated values",
                () -> assertEquals(-0.0, eiDTO.getAmount()),
                () -> assertEquals(0, eiDTO.getQuantity()));
    }

    @Test
    public void negateAmountAndMinus() {
        ETFInvestmentDTO eiDTO = ETFInvestmentDTO.builder().amount(-78.1).quantity(-6123).build();
        eiDTO.negateAmountAndQuantity();

        Assertions.assertAll("Assert all negated values",
                () -> assertEquals(78.1, eiDTO.getAmount()),
                () -> assertEquals(6123, eiDTO.getQuantity()));
    }

    private ETFInvestmentDTO getBaseDTO() {
        return ETFInvestmentDTO.builder()
                .amount(15.0)
                .quantity(667)
                .id(1L)
                .buySell("B")
                .liveValue(55.4)
                .shortName("AMD")
                .valueDiff(77.8)
                .build();
    }
}