package eye.on.the.money.model.stock;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class RSUVestingSchedule {

    private RSUVestingSchedule() {
    }

    public static List<Tranche> tranches(LocalDate grantDate, int quantity, int vestingYears,
                                         VestingFrequency frequency) {
        int count = frequency.vestCount(vestingYears);
        long total = quantity;

        List<Tranche> tranches = new ArrayList<>();
        long allocated = 0;
        for (int sequence = 1; sequence <= count; sequence++) {
            long cumulative = total * sequence / count;
            tranches.add(new Tranche(sequence,
                    grantDate.plusMonths((long) sequence * frequency.getMonthsBetweenVests()),
                    (int) (cumulative - allocated)));
            allocated = cumulative;
        }
        return tranches;
    }

    public static List<Tranche> tranches(RSUGrant grant) {
        return tranches(grant.getGrantDate(), grant.getQuantity(), grant.getVestingYears(),
                grant.getVestingFrequency());
    }

    public record Tranche(int sequence, LocalDate vestDate, int quantity) {
    }
}
