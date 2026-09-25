package eye.on.the.money.dto.out;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@ActiveProfiles("test")
class YearlyCashFlowDTOTest {

    @Test
    void add_accumulatesMonthsIntoTheYear() {
        YearlyCashFlowDTO year = YearlyCashFlowDTO.empty(2024, "HUF");

        year.add(MonthlyCashFlowDTO.builder().year(2024).month(3).currencyId("HUF")
                .moneyIn(new BigDecimal("1000")).moneyOut(new BigDecimal("-400")).build());
        year.add(MonthlyCashFlowDTO.builder().year(2024).month(4).currencyId("HUF")
                .moneyIn(new BigDecimal("500")).moneyOut(new BigDecimal("-700")).build());

        Assertions.assertAll("Assert accumulated values",
                () -> assertDecimal("1500", year.getMoneyIn()),
                () -> assertDecimal("-1100", year.getMoneyOut()),
                () -> assertDecimal("400", year.getNet()),
                () -> assertEquals(2, year.getMonthsCounted()));
    }

    @Test
    void getSavedPercent_reportsWhatShareOfIncomeWasKept() {
        YearlyCashFlowDTO year = this.year("1000000", "-400000", 12);

        assertDecimal("60", year.getSavedPercent());
    }

    @Test
    void getSavedPercent_isBlankWhenNothingCameIn() {
        assertNull(this.year("0", "-400", 2).getSavedPercent());
    }

    @Test
    void averages_divideByTheMonthsThatHadData() {
        YearlyCashFlowDTO year = this.year("1200", "-900", 3);

        Assertions.assertAll("Assert per-month averages",
                () -> assertDecimal("100", year.getAverageMonthlyNet()),
                () -> assertDecimal("-300", year.getAverageMonthlySpending()));
    }

    @Test
    void averages_areBlankWithoutMonths() {
        YearlyCashFlowDTO year = this.year("0", "0", 0);

        Assertions.assertAll("Assert blank averages",
                () -> assertNull(year.getAverageMonthlyNet()),
                () -> assertNull(year.getAverageMonthlySpending()));
    }

    @Test
    void getCSVRecord_matchesTheHeaders() {
        YearlyCashFlowDTO year = this.year("1200", "-900", 3);

        Assertions.assertAll("Assert CSV record",
                () -> assertEquals(year.getHeaders().length, year.getCSVRecord().length),
                () -> assertEquals(2023, year.getCSVRecord()[0]),
                () -> assertEquals("EUR", year.getCSVRecord()[1]),
                () -> assertEquals(3, year.getCSVRecord()[2]),
                () -> assertDecimal("300", (BigDecimal) year.getCSVRecord()[5]),
                () -> assertDecimal("25", (BigDecimal) year.getCSVRecord()[6]),
                () -> assertDecimal("-300", (BigDecimal) year.getCSVRecord()[8]));
    }

    private YearlyCashFlowDTO year(String moneyIn, String moneyOut, int months) {
        return YearlyCashFlowDTO.builder().year(2023).currencyId("EUR")
                .moneyIn(new BigDecimal(moneyIn)).moneyOut(new BigDecimal(moneyOut)).monthsCounted(months).build();
    }

    private static void assertDecimal(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual), () -> "expected " + expected + " but was " + actual);
    }
}
