package eye.on.the.money.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;

import static eye.on.the.money.security.SecurityConstants.EXPIRATION;
import static eye.on.the.money.security.SecurityConstants.KEY;
import static eye.on.the.money.security.SecurityConstants.MFA_EXPIRATION;
import static eye.on.the.money.security.SecurityConstants.TOKEN_TYPE_CLAIM;

@Service
public class JwtService {

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
        var key = Keys.hmacShaKeyFor(KEY.getBytes());

        return Jwts.builder()
                .subject(email)
                .claim(TOKEN_TYPE_CLAIM, type.claimValue())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(amount, unit)))
                .signWith(key)
                .compact();
    }

    private Claims getTokenBody(String token) {
        var key = Keys.hmacShaKeyFor(KEY.getBytes());
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
