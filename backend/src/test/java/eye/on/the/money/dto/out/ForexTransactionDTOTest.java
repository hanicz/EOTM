package eye.on.the.money.dto.out;

import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
class ForexTransactionDTOTest {

    @Mock
    private CSVRecord record;

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

    @Test
    public void getHeaders() {
        ForexTransactionDTO dto = ForexTransactionDTO.builder().build();

        Assertions.assertAll("Assert all headers",
                () -> assertEquals("Transaction Id", dto.getHeaders()[0]),
                () -> assertEquals("From Amount", dto.getHeaders()[1]),
                () -> assertEquals("To Amount", dto.getHeaders()[2]),
                () -> assertEquals("Type", dto.getHeaders()[3]),
                () -> assertEquals("Transaction Date", dto.getHeaders()[4]),
                () -> assertEquals("Change Rate", dto.getHeaders()[5]),
                () -> assertEquals("From Currency", dto.getHeaders()[6]),
                () -> assertEquals("To Currency", dto.getHeaders()[7])
        );
    }

    @Test
    public void getCSVRecord() {
        LocalDate ld = LocalDate.now();
        ForexTransactionDTO dto = this.getBaseTrans();
        dto.setTransactionDate(ld);

        Assertions.assertAll("Assert all headers",
                () -> assertEquals(1L, dto.getCSVRecord()[0]),
                () -> assertEquals(10.5, dto.getCSVRecord()[1]),
                () -> assertEquals(50.2, dto.getCSVRecord()[2]),
                () -> assertEquals("F", dto.getCSVRecord()[3]),
                () -> assertEquals(ld, dto.getCSVRecord()[4]),
                () -> assertEquals(0.5, dto.getCSVRecord()[5]),
                () -> assertEquals("USD", dto.getCSVRecord()[6]),
                () -> assertEquals("EUR", dto.getCSVRecord()[7])
        );
    }

    @Test
    public void createFromCSVRecord() {
        when(this.record.get("Transaction Date")).thenReturn("2020-01-01");
        when(this.record.get("Transaction Id")).thenReturn("1");
        when(this.record.get("Type")).thenReturn("F");
        when(this.record.get("From Amount")).thenReturn("10.5");
        when(this.record.get("To Amount")).thenReturn("50.2");
        when(this.record.get("Change Rate")).thenReturn("0.5");
        when(this.record.get("From Currency")).thenReturn("USD");
        when(this.record.get("To Currency")).thenReturn("EUR");

        ForexTransactionDTO tDTO = ForexTransactionDTO.createFromCSVRecord(this.record, DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        Assertions.assertAll("Assert all values",
                () -> assertEquals(1L, tDTO.getForexTransactionId()),
                () -> assertEquals(10.5, tDTO.getFromAmount()),
                () -> assertEquals(50.2, tDTO.getToAmount()),
                () -> assertEquals("F", tDTO.getBuySell()),
                () -> assertEquals(LocalDate.parse("2020-01-01"), tDTO.getTransactionDate()),
                () -> assertEquals(0.5, tDTO.getChangeRate()),
                () -> assertEquals("USD", tDTO.getFromCurrencyId()),
                () -> assertEquals("EUR", tDTO.getToCurrencyId())
        );
    }

    private ForexTransactionDTO getBaseTrans() {
        return ForexTransactionDTO.builder()
                .forexTransactionId(1L)
                .fromAmount(10.5)
                .toAmount(50.2)
                .buySell("F")
                .changeRate(0.5)
                .fromCurrencyId("USD")
                .toCurrencyId("EUR")
                .build();
    }
}