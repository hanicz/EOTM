package eye.on.the.money.security;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class JwtServiceTest {

    private static final String EMAIL = "tokenholder@mail.com";

    private final JwtService jwtService = new JwtService();

    @Test
    public void accessTokenCarriesTheSubject() {
        String token = this.jwtService.generateAccessToken(EMAIL);

        assertEquals(EMAIL, this.jwtService.extractUsername(token, TokenType.ACCESS));
    }

    @Test
    public void challengeTokenCarriesTheSubject() {
        String token = this.jwtService.generateChallengeToken(EMAIL);

        assertEquals(EMAIL, this.jwtService.extractUsername(token, TokenType.MFA));
    }

    @Test
    public void aChallengeTokenIsNotAnAccessToken() {
        String token = this.jwtService.generateChallengeToken(EMAIL);

        assertThrows(JwtException.class, () -> this.jwtService.extractUsername(token, TokenType.ACCESS));
    }

    @Test
    public void anAccessTokenIsNotAChallengeToken() {
        String token = this.jwtService.generateAccessToken(EMAIL);

        assertThrows(JwtException.class, () -> this.jwtService.extractUsername(token, TokenType.MFA));
    }

    @Test
    public void theTwoTokenTypesDiffer() {
        assertNotEquals(this.jwtService.generateAccessToken(EMAIL), this.jwtService.generateChallengeToken(EMAIL));
    }

    @Test
    public void garbageIsRejected() {
        assertThrows(JwtException.class, () -> this.jwtService.extractUsername("not.a.token", TokenType.ACCESS));
    }
}
