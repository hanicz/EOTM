package eye.on.the.money.dto.out;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@ActiveProfiles("test")
class YearlyCashFlowDTOTest {

    @Test
    void add_accumulatesMonthsIntoTheYear() {
        YearlyCashFlowDTO year = YearlyCashFlowDTO.empty(2024, "HUF");

        year.add(MonthlyCashFlowDTO.builder().year(2024).month(3).currencyId("HUF")
                .moneyIn(1000.0).moneyOut(-400.0).build());
        year.add(MonthlyCashFlowDTO.builder().year(2024).month(4).currencyId("HUF")
                .moneyIn(500.0).moneyOut(-700.0).build());

        Assertions.assertAll("Assert accumulated values",
                () -> assertEquals(1500.0, year.getMoneyIn()),
                () -> assertEquals(-1100.0, year.getMoneyOut()),
                () -> assertEquals(400.0, year.getNet()),
                () -> assertEquals(2, year.getMonthsCounted()));
    }

    @Test
    void getSavedPercent_reportsWhatShareOfIncomeWasKept() {
        YearlyCashFlowDTO year = this.year(1000000.0, -400000.0, 12);

        assertEquals(60.0, year.getSavedPercent());
    }

    @Test
    void getSavedPercent_isBlankWhenNothingCameIn() {
        assertNull(this.year(0.0, -400.0, 2).getSavedPercent());
    }

    @Test
    void averages_divideByTheMonthsThatHadData() {
        YearlyCashFlowDTO year = this.year(1200.0, -900.0, 3);

        Assertions.assertAll("Assert per-month averages",
                () -> assertEquals(100.0, year.getAverageMonthlyNet()),
                () -> assertEquals(-300.0, year.getAverageMonthlySpending()));
    }

    @Test
    void averages_areBlankWithoutMonths() {
        YearlyCashFlowDTO year = this.year(0.0, 0.0, 0);

        Assertions.assertAll("Assert blank averages",
                () -> assertNull(year.getAverageMonthlyNet()),
                () -> assertNull(year.getAverageMonthlySpending()));
    }

    @Test
    void getCSVRecord_matchesTheHeaders() {
        YearlyCashFlowDTO year = this.year(1200.0, -900.0, 3);

        Assertions.assertAll("Assert CSV record",
                () -> assertEquals(year.getHeaders().length, year.getCSVRecord().length),
                () -> assertEquals(2023, year.getCSVRecord()[0]),
                () -> assertEquals("EUR", year.getCSVRecord()[1]),
                () -> assertEquals(3, year.getCSVRecord()[2]),
                () -> assertEquals(300.0, year.getCSVRecord()[5]),
                () -> assertEquals(25.0, year.getCSVRecord()[6]),
                () -> assertEquals(-300.0, year.getCSVRecord()[8]));
    }

    private YearlyCashFlowDTO year(double moneyIn, double moneyOut, int months) {
        return YearlyCashFlowDTO.builder().year(2023).currencyId("EUR")
                .moneyIn(moneyIn).moneyOut(moneyOut).monthsCounted(months).build();
    }
}
