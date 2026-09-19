package eye.on.the.money.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HungarianPensionScaleTest {

    private static final double TOLERANCE = 0.0001;

    @Test
    void percentFor_returnsNothingBelowThePartialThreshold() {
        assertEquals(0, HungarianPensionScale.percentFor(0), TOLERANCE);
        assertEquals(0, HungarianPensionScale.percentFor(10), TOLERANCE);
        assertEquals(0, HungarianPensionScale.percentFor(14), TOLERANCE);
    }

    @Test
    void percentFor_risesByTwoPointsAYearFromFifteenToTwentyFive() {
        assertEquals(43, HungarianPensionScale.percentFor(15), TOLERANCE);
        assertEquals(45, HungarianPensionScale.percentFor(16), TOLERANCE);
        assertEquals(53, HungarianPensionScale.percentFor(20), TOLERANCE);
        assertEquals(63, HungarianPensionScale.percentFor(25), TOLERANCE);
    }

    @Test
    void percentFor_risesByOnePointAYearFromTwentySixToThirtySix() {
        assertEquals(64, HungarianPensionScale.percentFor(26), TOLERANCE);
        assertEquals(68, HungarianPensionScale.percentFor(30), TOLERANCE);
        assertEquals(73, HungarianPensionScale.percentFor(35), TOLERANCE);
        assertEquals(74, HungarianPensionScale.percentFor(36), TOLERANCE);
    }

    @Test
    void percentFor_risesByOneAndAHalfPointsAYearFromThirtySevenToForty() {
        assertEquals(75.5, HungarianPensionScale.percentFor(37), TOLERANCE);
        assertEquals(77, HungarianPensionScale.percentFor(38), TOLERANCE);
        assertEquals(78.5, HungarianPensionScale.percentFor(39), TOLERANCE);
        assertEquals(80, HungarianPensionScale.percentFor(40), TOLERANCE);
    }

    @Test
    void percentFor_risesByTwoPointsAYearAgainAboveForty() {
        assertEquals(82, HungarianPensionScale.percentFor(41), TOLERANCE);
        assertEquals(90, HungarianPensionScale.percentFor(45), TOLERANCE);
        assertEquals(98, HungarianPensionScale.percentFor(49), TOLERANCE);
    }

    @Test
    void percentFor_stopsAtOneHundred() {
        assertEquals(100, HungarianPensionScale.percentFor(50), TOLERANCE);
        assertEquals(100, HungarianPensionScale.percentFor(60), TOLERANCE);
        assertEquals(100, HungarianPensionScale.percentFor(200), TOLERANCE);
    }

    @Test
    void pensionNet_takesContributionsFirstAndTaxOnWhatIsLeft() {
        assertEquals(692_750, HungarianPensionScale.pensionNet(1_000_000), 0.01);
    }

    @Test
    void pensionNet_isMoreGenerousThanThePayrollNet() {
        double payrollNet = 1_000_000 * (1 - HungarianPensionScale.TB_RATE - HungarianPensionScale.SZJA_RATE);
        assertEquals(665_000, payrollNet, 0.01);
    }
}
