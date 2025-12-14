package eye.on.the.money.dto.out;

import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class ETFInvestmentDTOTest {

    @Mock
    private CSVRecord record;

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

    @Test
    public void getHeaders() {
        ETFInvestmentDTO eiDTO = ETFInvestmentDTO.builder().build();

        Assertions.assertAll("Assert all headers",
                () -> assertEquals("Investment Id", eiDTO.getHeaders()[0]),
                () -> assertEquals("Quantity", eiDTO.getHeaders()[1]),
                () -> assertEquals("Type", eiDTO.getHeaders()[2]),
                () -> assertEquals("Transaction Date", eiDTO.getHeaders()[3]),
                () -> assertEquals("Short Name", eiDTO.getHeaders()[4]),
                () -> assertEquals("Exchange", eiDTO.getHeaders()[5]),
                () -> assertEquals("Amount", eiDTO.getHeaders()[6]),
                () -> assertEquals("Currency", eiDTO.getHeaders()[7]),
                () -> assertEquals("Fee", eiDTO.getHeaders()[8])
        );
    }

    @Test
    public void getCSVRecord() {
        LocalDate ld = LocalDate.now();
        ETFInvestmentDTO eiDTO = this.getBaseDTO();
        eiDTO.setTransactionDate(ld);

        Assertions.assertAll("Assert all headers",
                () -> assertEquals(1L, eiDTO.getCSVRecord()[0]),
                () -> assertEquals(667, eiDTO.getCSVRecord()[1]),
                () -> assertEquals("B", eiDTO.getCSVRecord()[2]),
                () -> assertEquals(ld, eiDTO.getCSVRecord()[3]),
                () -> assertEquals("AMD", eiDTO.getCSVRecord()[4]),
                () -> assertEquals("NASDAQ", eiDTO.getCSVRecord()[5]),
                () -> assertEquals(15.0, eiDTO.getCSVRecord()[6]),
                () -> assertEquals("USD", eiDTO.getCSVRecord()[7]),
                () -> assertEquals(1.2, eiDTO.getCSVRecord()[8])

        );
    }

    @Test
    public void createFromCSVRecord() {
        when(this.record.get("Investment Id")).thenReturn("1");
        when(this.record.get("Quantity")).thenReturn("667");
        when(this.record.get("Type")).thenReturn("B");
        when(this.record.get("Transaction Date")).thenReturn("2020-01-01");
        when(this.record.get("Short Name")).thenReturn("AMD");
        when(this.record.get("Exchange")).thenReturn("NASDAQ");
        when(this.record.get("Amount")).thenReturn("15.0");
        when(this.record.get("Currency")).thenReturn("USD");
        when(this.record.get("Fee")).thenReturn("1.2");

        ETFInvestmentDTO eiDTO = ETFInvestmentDTO.createFromCSVRecord(this.record, DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        Assertions.assertAll("Assert all values",
                () -> assertEquals(1L, eiDTO.getId()),
                () -> assertEquals(667, eiDTO.getQuantity()),
                () -> assertEquals("B", eiDTO.getBuySell()),
                () -> assertEquals(LocalDate.parse("2020-01-01"), eiDTO.getTransactionDate()),
                () -> assertEquals("AMD", eiDTO.getShortName()),
                () -> assertEquals("NASDAQ", eiDTO.getExchange()),
                () -> assertEquals(15.0, eiDTO.getAmount()),
                () -> assertEquals("USD", eiDTO.getCurrencyId()),
                () -> assertEquals(1.2, eiDTO.getFee())
        );
    }

    private ETFInvestmentDTO getBaseDTO() {
        return ETFInvestmentDTO.builder()
                .amount(15.0)
                .quantity(667)
                .id(1L)
                .buySell("B")
                .liveValue(55.4)
                .shortName("AMD")
                .currencyId("USD")
                .valueDiff(77.8)
                .exchange("NASDAQ")
                .fee(1.2)
                .build();
    }
}