package eye.on.the.money.security;

public class SecurityConstants {
    public static final String SIGN_UP_URL = "/api/v1/user/signup";
    public static final String TOTP_VERIFY_URL = "/api/v1/auth/2fa/verify";
    public static final String KEY = System.getenv("EOTM_KEY");
    public static final String HEADER_NAME = "Authorization";
    public static final Integer EXPIRATION = 72;
    public static final Integer MFA_EXPIRATION = 5;
    public static final String TOKEN_TYPE_CLAIM = "typ";
    public static final String[] SPA_ROUTES = {
            "/login", "/dashboard", "/news", "/stock", "/crypto", "/watchlist", "/search",
            "/forex", "/security", "/etf", "/cash", "/alert", "/tax", "/fire", "/financial", "/history", "/salary",
            "/equity", "/settings", "/pension", "/performance"
    };
}
