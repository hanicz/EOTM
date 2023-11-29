package eye.on.the.money.dto.out;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ActiveProfiles("test")
class InvestmentDTOTest {

    @Test
    public void mergeInvestments() {
        InvestmentDTO iDTO1 = this.getBaseDTO();
        InvestmentDTO iDTO2 = InvestmentDTO.builder()
                .amount(15.0)
                .quantity(667)
                .buySell("B")
                .shortName("AMD")
                .build();
        InvestmentDTO baseDTO = this.getBaseDTO();

        iDTO1.mergeInvestments(iDTO2);

        Assertions.assertAll("Assert all changing values",
                () -> assertEquals(iDTO2.getAmount() + baseDTO.getAmount(), iDTO1.getAmount()),
                () -> assertEquals(iDTO2.getQuantity() + baseDTO.getQuantity(), iDTO1.getQuantity()),
                () -> assertEquals("B", iDTO1.getBuySell()));
    }

    @Test
    public void mergeInvestmentsWithSell() {
        InvestmentDTO iDTO1 = this.getBaseDTO();
        iDTO1.setBuySell("S");
        InvestmentDTO iDTO2 = InvestmentDTO.builder()
                .amount(10.0)
                .quantity(6)
                .buySell("B")
                .shortName("AMD")
                .build();
        InvestmentDTO baseDTO = this.getBaseDTO();

        iDTO1.mergeInvestments(iDTO2);

        Assertions.assertAll("Assert all changing values",
                () -> assertEquals(iDTO2.getAmount() + baseDTO.getAmount(), iDTO1.getAmount()),
                () -> assertEquals(iDTO2.getQuantity() + baseDTO.getQuantity(), iDTO1.getQuantity()),
                () -> assertEquals("B", iDTO1.getBuySell()));
    }

    @Test
    public void mergeInvestmentsDifferentShortName() {
        InvestmentDTO iDTO1 = this.getBaseDTO();
        InvestmentDTO iDTO2 = InvestmentDTO.builder()
                .amount(15.0)
                .quantity(667)
                .buySell("B")
                .shortName("CRSR")
                .build();

        iDTO1.mergeInvestments(iDTO2);

        Assertions.assertAll("Assert all changing values",
                () -> assertEquals(15.0, iDTO1.getAmount()),
                () -> assertEquals(667, iDTO1.getQuantity()),
                () -> assertEquals("B", iDTO1.getBuySell()));
    }

    @Test
    public void negateAmountAndQuantity() {
        InvestmentDTO iDTO = InvestmentDTO.builder().amount(15.0).quantity(667).build();
        iDTO.negateAmountAndQuantity();

        Assertions.assertAll("Assert all negated values",
                () -> assertEquals(-15.0, iDTO.getAmount()),
                () -> assertEquals(-667, iDTO.getQuantity()));
    }

    @Test
    public void negateAmountAndZero() {
        InvestmentDTO iDTO = InvestmentDTO.builder().amount(0.0).quantity(0).build();
        iDTO.negateAmountAndQuantity();

        Assertions.assertAll("Assert all negated values",
                () -> assertEquals(-0.0, iDTO.getAmount()),
                () -> assertEquals(0, iDTO.getQuantity()));
    }

    @Test
    public void negateAmountAndMinus() {
        InvestmentDTO iDTO = InvestmentDTO.builder().amount(-78.1).quantity(-6123).build();
        iDTO.negateAmountAndQuantity();

        Assertions.assertAll("Assert all negated values",
                () -> assertEquals(78.1, iDTO.getAmount()),
                () -> assertEquals(6123, iDTO.getQuantity()));
    }

    private InvestmentDTO getBaseDTO() {
        return InvestmentDTO.builder()
                .amount(15.0)
                .quantity(667)
                .buySell("B")
                .shortName("AMD")
                .build();
    }
}