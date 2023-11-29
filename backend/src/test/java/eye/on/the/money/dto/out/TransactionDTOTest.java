package eye.on.the.money.dto.out;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
class TransactionDTOTest {

    @Test
    public void mergeInvestments() {
        TransactionDTO tDTO1 = this.getBaseDTO();
        TransactionDTO tDTO2 = TransactionDTO.builder()
                .amount(15.0)
                .quantity(667.4)
                .buySell("B")
                .symbol("BTC")
                .build();
        TransactionDTO baseDTO = this.getBaseDTO();

        tDTO1.mergeTransactions(tDTO2);

        Assertions.assertAll("Assert all changing values",
                () -> assertEquals(tDTO2.getAmount() + baseDTO.getAmount(), tDTO1.getAmount()),
                () -> assertEquals(tDTO2.getQuantity() + baseDTO.getQuantity(), tDTO1.getQuantity()),
                () -> assertEquals("B", tDTO1.getBuySell()));
    }

    @Test
    public void mergeInvestmentsWithSell() {
        TransactionDTO tDTO1 = this.getBaseDTO();
        tDTO1.setBuySell("S");
        TransactionDTO tDTO2 = TransactionDTO.builder()
                .amount(10.0)
                .quantity(6.0)
                .buySell("B")
                .symbol("BTC")
                .build();
        TransactionDTO baseDTO = this.getBaseDTO();

        tDTO1.mergeTransactions(tDTO2);

        Assertions.assertAll("Assert all changing values",
                () -> assertEquals(tDTO2.getAmount() + baseDTO.getAmount(), tDTO1.getAmount()),
                () -> assertEquals(tDTO2.getQuantity() + baseDTO.getQuantity(), tDTO1.getQuantity()),
                () -> assertEquals("B", tDTO1.getBuySell()));
    }

    @Test
    public void mergeInvestmentsDifferentSymbol() {
        TransactionDTO tDTO1 = this.getBaseDTO();
        TransactionDTO tDTO2 = TransactionDTO.builder()
                .amount(15.0)
                .quantity(667.4)
                .buySell("B")
                .symbol("ETH")
                .build();

        tDTO1.mergeTransactions(tDTO2);

        Assertions.assertAll("Assert all changing values",
                () -> assertEquals(15.0, tDTO1.getAmount()),
                () -> assertEquals(667.4, tDTO1.getQuantity()),
                () -> assertEquals("B", tDTO1.getBuySell()));
    }

    @Test
    public void negateAmountAndQuantity() {
        TransactionDTO tDTO = TransactionDTO.builder().amount(15.0).quantity(667.2).build();
        tDTO.negateAmountAndQuantity();

        Assertions.assertAll("Assert all negated values",
                () -> assertEquals(-15.0, tDTO.getAmount()),
                () -> assertEquals(-667.2, tDTO.getQuantity()));
    }

    @Test
    public void negateAmountAndZero() {
        TransactionDTO tDTO = TransactionDTO.builder().amount(0.0).quantity(0.0).build();
        tDTO.negateAmountAndQuantity();

        Assertions.assertAll("Assert all negated values",
                () -> assertEquals(-0.0, tDTO.getAmount()),
                () -> assertEquals(-0.0, tDTO.getQuantity()));
    }

    @Test
    public void negateAmountAndMinus() {
        TransactionDTO tDTO = TransactionDTO.builder().amount(-78.1).quantity(-6123.3).build();
        tDTO.negateAmountAndQuantity();

        Assertions.assertAll("Assert all negated values",
                () -> assertEquals(78.1, tDTO.getAmount()),
                () -> assertEquals(6123.3, tDTO.getQuantity()));
    }

    private TransactionDTO getBaseDTO() {
        return TransactionDTO.builder()
                .amount(15.0)
                .quantity(667.4)
                .buySell("B")
                .symbol("BTC")
                .build();
    }
}