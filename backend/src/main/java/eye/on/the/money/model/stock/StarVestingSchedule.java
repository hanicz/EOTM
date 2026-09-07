package eye.on.the.money.model.stock;

import java.time.LocalDate;
import java.time.MonthDay;
import java.util.ArrayList;
import java.util.List;

public final class StarVestingSchedule {

    public static final int QUARTERS_IN_YEAR = 4;
    public static final int MONTHS_BETWEEN_QUARTERS = 3;
    public static final int CLIFF_QUARTERS = 4;

    private static final List<MonthDay> COMMENCEMENT_DAYS = List.of(
            MonthDay.of(1, 8), MonthDay.of(4, 8), MonthDay.of(7, 8), MonthDay.of(10, 8));

    private StarVestingSchedule() {
    }

    public static LocalDate nextCommencementDate(LocalDate date) {
        for (int year = date.getYear(); year <= date.getYear() + 1; year++) {
            for (MonthDay day : COMMENCEMENT_DAYS) {
                LocalDate candidate = day.atYear(year);
                if (!candidate.isBefore(date)) {
                    return candidate;
                }
            }
        }
        throw new IllegalStateException("No vesting commencement date on or after " + date);
    }

    public static List<Tranche> tranches(LocalDate commencement, int quantity, int vestingYears) {
        int totalQuarters = vestingYears * QUARTERS_IN_YEAR;
        long total = quantity;

        List<Tranche> tranches = new ArrayList<>();
        long allocated = 0;
        int sequence = 0;
        for (int quarter = CLIFF_QUARTERS; quarter <= totalQuarters; quarter++) {
            long cumulative = total * quarter / totalQuarters;
            tranches.add(new Tranche(++sequence,
                    commencement.plusMonths((long) quarter * MONTHS_BETWEEN_QUARTERS),
                    (int) (cumulative - allocated), quarter == CLIFF_QUARTERS));
            allocated = cumulative;
        }
        return tranches;
    }

    public record Tranche(int sequence, LocalDate vestDate, int quantity, boolean cliff) {
    }
}
