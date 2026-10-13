package eye.on.the.money.model.stock;

import eye.on.the.money.model.stock.RSUVestingSchedule.Tranche;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RSUVestingScheduleTest {

    private static final LocalDate GRANT_DATE = LocalDate.of(2022, 2, 15);

    @Test
    void splitsAnAnnualGrantIntoEqualYearlyTranches() {
        List<Tranche> tranches = RSUVestingSchedule.tranches(GRANT_DATE, 100, 4, VestingFrequency.ANNUAL);

        assertEquals(List.of(
                new Tranche(1, LocalDate.of(2023, 2, 15), 25),
                new Tranche(2, LocalDate.of(2024, 2, 15), 25),
                new Tranche(3, LocalDate.of(2025, 2, 15), 25),
                new Tranche(4, LocalDate.of(2026, 2, 15), 25)), tranches);
    }

    @Test
    void spreadsTheRemainderOfAQuarterlyGrantSoTheTotalIsKept() {
        List<Tranche> tranches = RSUVestingSchedule.tranches(GRANT_DATE, 10, 1, VestingFrequency.QUARTERLY);

        assertEquals(List.of(2, 3, 2, 3), tranches.stream().map(Tranche::quantity).toList());
        assertEquals(LocalDate.of(2022, 5, 15), tranches.getFirst().vestDate());
        assertEquals(LocalDate.of(2023, 2, 15), tranches.getLast().vestDate());
    }
}
