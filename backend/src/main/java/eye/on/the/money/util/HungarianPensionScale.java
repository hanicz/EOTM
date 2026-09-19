package eye.on.the.money.util;

public final class HungarianPensionScale {

    public static final int EARNINGS_START_YEAR = 1988;
    public static final int MIN_SERVICE_YEARS_PARTIAL = 15;
    public static final int MIN_SERVICE_YEARS_FULL = 20;
    public static final int DEFAULT_RETIREMENT_AGE = 65;

    public static final double MINIMUM_PENSION = 28_500;
    public static final double DEGRESSIO_LOWER = 372_000;
    public static final double DEGRESSIO_UPPER = 421_000;
    public static final double DEGRESSIO_UPPER_RATE = 0.9;
    public static final double DEGRESSIO_TOP_RATE = 0.8;

    public static final double TB_RATE = 0.185;
    public static final double SZJA_RATE = 0.15;

    public static final int MONTHS_IN_YEAR = 12;
    public static final int THIRTEENTH_MONTH_FACTOR = 13;

    public static final double NATIONAL_AVERAGE_GROSS_MONTHLY = 754_700;

    public static final double DEFAULT_REAL_WAGE_GROWTH = 1.5;
    public static final double DEFAULT_INFLATION = FireDefaults.INFLATION;

    private static final int FIRST_SCALED_YEAR = 10;
    private static final int LAST_SCALED_YEAR = 50;

    private static final double[] PERCENTS = {
            33, 35, 37, 39, 41, 43, 45, 47, 49, 51,
            53, 55, 57, 59, 61, 63, 64, 65, 66, 67,
            68, 69, 70, 71, 72, 73, 74, 75.5, 77, 78.5,
            80, 82, 84, 86, 88, 90, 92, 94, 96, 98, 100};

    private HungarianPensionScale() {
    }

    public static double percentFor(int serviceYears) {
        if (serviceYears < MIN_SERVICE_YEARS_PARTIAL) {
            return 0;
        }
        return PERCENTS[Math.min(serviceYears, LAST_SCALED_YEAR) - FIRST_SCALED_YEAR];
    }

    public static double pensionNet(double gross) {
        return gross * (1 - TB_RATE) * (1 - SZJA_RATE);
    }
}
