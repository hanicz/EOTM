package eye.on.the.money.dto.out;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ActiveProfiles("test")
class ForexTransactionDTOTest {

    @Test
    public void mergeTransactions() {
        ForexTransactionDTO dto1 = this.getBaseTrans();
        ForexTransactionDTO dto2 = ForexTransactionDTO.builder().fromAmount(20.3).toAmount(70.9).build();

        ForexTransactionDTO baseDTO = this.getBaseTrans();

        dto1.mergeTransactions(dto2);

        Assertions.assertAll("Assert all changing values",
                () -> assertEquals(baseDTO.getFromAmount() + dto2.getFromAmount(), dto1.getFromAmount()),
                () -> assertEquals(baseDTO.getToAmount() + dto2.getToAmount(), dto1.getToAmount()),
                () -> assertEquals(dto1.getFromAmount() / dto1.getToAmount(), dto1.getChangeRate()));
    }

    private ForexTransactionDTO getBaseTrans() {
        return ForexTransactionDTO.builder().fromAmount(10.5).toAmount(50.2).build();
    }
}