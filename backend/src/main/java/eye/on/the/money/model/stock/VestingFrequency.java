package eye.on.the.money.model.stock;

public enum VestingFrequency {

    ANNUAL(12),
    QUARTERLY(3);

    private static final int MONTHS_IN_YEAR = 12;

    private final int monthsBetweenVests;

    VestingFrequency(int monthsBetweenVests) {
        this.monthsBetweenVests = monthsBetweenVests;
    }

    public int getMonthsBetweenVests() {
        return this.monthsBetweenVests;
    }

    public int vestCount(int vestingYears) {
        return vestingYears * MONTHS_IN_YEAR / this.monthsBetweenVests;
    }
}
