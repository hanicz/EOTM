package eye.on.the.money.security;

public class SecurityConstants {
    public static final String SIGN_UP_URL = "/user/signup";
    public static final String H2_CONSOLE = "/h2-console/*";
    public static final String KEY = System.getenv("EOTM_KEY");
    public static final String HEADER_NAME = "Authorization";
    public static final Integer EXPIRATION = 60;
}
