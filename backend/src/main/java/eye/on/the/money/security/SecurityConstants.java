package eye.on.the.money.security;

public class SecurityConstants {
    public static final String SIGN_UP_URL = "/api/v1/user/signup";
    public static final String KEY = System.getenv("EOTM_KEY");
    public static final String HEADER_NAME = "Authorization";
    public static final Integer EXPIRATION = 1440;
    public static final String[] SPA_ROUTES = {
            "/login", "/dashboard", "/news", "/stock", "/crypto", "/watchlist", "/search",
            "/forex", "/security", "/etf", "/alert", "/tax", "/fire", "/financial", "/settings"
    };
}
