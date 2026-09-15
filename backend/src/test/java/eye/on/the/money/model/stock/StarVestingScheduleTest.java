package eye.on.the.money.model.stock;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StarVestingScheduleTest {

    private static final LocalDate COMMENCEMENT = LocalDate.of(2030, 4, 8);

    @Test
    void cliffScheduleVestsAQuarterAfterOneYearThenQuarterly() {
        List<StarVestingSchedule.Tranche> tranches =
                StarVestingSchedule.tranches(COMMENCEMENT, 1600, 4, StarVestingType.CLIFF);

        assertEquals(13, tranches.size());
        assertEquals(LocalDate.of(2031, 4, 8), tranches.get(0).vestDate());
        assertEquals(400, tranches.get(0).quantity());
        assertTrue(tranches.get(0).cliff());
        assertEquals(LocalDate.of(2031, 7, 8), tranches.get(1).vestDate());
        tranches.subList(1, tranches.size()).forEach(tranche -> {
            assertEquals(100, tranche.quantity());
            assertFalse(tranche.cliff());
        });
        assertEquals(LocalDate.of(2034, 4, 8), tranches.get(12).vestDate());
    }

    @Test
    void noCliffScheduleVestsEveryQuarterFromTheStart() {
        List<StarVestingSchedule.Tranche> tranches =
                StarVestingSchedule.tranches(COMMENCEMENT, 1600, 4, StarVestingType.NO_CLIFF);

        assertEquals(16, tranches.size());
        assertEquals(LocalDate.of(2030, 7, 8), tranches.get(0).vestDate());
        tranches.forEach(tranche -> {
            assertEquals(100, tranche.quantity());
            assertFalse(tranche.cliff());
        });
        int vestedAfterOneYear = tranches.stream()
                .filter(tranche -> !tranche.vestDate().isAfter(LocalDate.of(2031, 4, 8)))
                .mapToInt(StarVestingSchedule.Tranche::quantity).sum();
        assertEquals(400, vestedAfterOneYear);
        assertEquals(LocalDate.of(2034, 4, 8), tranches.get(15).vestDate());
    }

    @Test
    void unevenQuantityIsFullyAllocatedForEveryType() {
        for (StarVestingType type : StarVestingType.values()) {
            int allocated = StarVestingSchedule.tranches(COMMENCEMENT, 1601, 4, type).stream()
                    .mapToInt(StarVestingSchedule.Tranche::quantity).sum();
            assertEquals(1601, allocated);
        }
    }

    @Test
    void noCliffGrantFromMidMarchFirstVestsInJuly() {
        LocalDate commencement = StarVestingSchedule.nextCommencementDate(LocalDate.of(2030, 3, 13));
        StarVestingSchedule.Tranche first =
                StarVestingSchedule.tranches(commencement, 1000, 4, StarVestingType.NO_CLIFF).getFirst();

        assertEquals(LocalDate.of(2030, 4, 8), commencement);
        assertEquals(LocalDate.of(2030, 7, 8), first.vestDate());
        assertEquals(62, first.quantity());
    }
}
