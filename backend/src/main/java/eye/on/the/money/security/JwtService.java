package eye.on.the.money.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.sql.Date;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;

import static eye.on.the.money.security.SecurityConstants.EXPIRATION;
import static eye.on.the.money.security.SecurityConstants.MFA_EXPIRATION;
import static eye.on.the.money.security.SecurityConstants.TOKEN_TYPE_CLAIM;

@Service
public class JwtService {

    private final SecretKey key;

    public JwtService(@Value("${EOTM_KEY}") String key) {
        this.key = Keys.hmacShaKeyFor(key.getBytes());
    }

    public String generateAccessToken(String email) {
        return this.generateToken(email, TokenType.ACCESS, EXPIRATION, ChronoUnit.HOURS);
    }

    public String generateChallengeToken(String email) {
        return this.generateToken(email, TokenType.MFA, MFA_EXPIRATION, ChronoUnit.MINUTES);
    }

    public String extractUsername(String token, TokenType expected) {
        Claims claims = this.getTokenBody(token);
        String type = claims.get(TOKEN_TYPE_CLAIM, String.class);
        if (!expected.claimValue().equals(type)) {
            throw new JwtException("Unexpected token type");
        }
        return claims.getSubject();
    }

    private String generateToken(String email, TokenType type, long amount, TemporalUnit unit) {
        var now = Instant.now();

        return Jwts.builder()
                .subject(email)
                .claim(TOKEN_TYPE_CLAIM, type.claimValue())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(amount, unit)))
                .signWith(this.key)
                .compact();
    }

    private Claims getTokenBody(String token) {
        return Jwts.parser()
                .verifyWith(this.key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
