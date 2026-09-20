package eye.on.the.money.security;

public enum TokenType {
    ACCESS("access"),
    MFA("mfa");

    private final String claimValue;

    TokenType(String claimValue) {
        this.claimValue = claimValue;
    }

    public String claimValue() {
        return this.claimValue;
    }
}
