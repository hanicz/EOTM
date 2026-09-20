package eye.on.the.money.controller;

import eye.on.the.money.dto.in.TotpVerifyDTO;
import eye.on.the.money.exception.TotpChallengeException;
import eye.on.the.money.security.JwtService;
import eye.on.the.money.security.TokenType;
import eye.on.the.money.service.user.TotpService;
import eye.on.the.money.util.LogSanitizer;
import io.jsonwebtoken.JwtException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private static final String REJECTED_MESSAGE = "Invalid or expired code";

    private final JwtService jwtService;

    private final TotpService totpService;

    @PostMapping("/2fa/verify")
    public ResponseEntity<Void> verifyTotp(@RequestBody @Valid TotpVerifyDTO verifyDTO) {
        String email = this.emailFromChallenge(verifyDTO.mfaToken());

        if (!this.totpService.verify(email, verifyDTO.code())) {
            throw new TotpChallengeException(REJECTED_MESSAGE);
        }

        log.info("Two-factor check passed for {}", LogSanitizer.maskEmail(email));
        return ResponseEntity.noContent().header("token", this.jwtService.generateAccessToken(email)).build();
    }

    private String emailFromChallenge(String mfaToken) {
        try {
            return this.jwtService.extractUsername(mfaToken, TokenType.MFA);
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Two-factor challenge rejected: {}", e.getMessage());
            throw new TotpChallengeException(REJECTED_MESSAGE);
        }
    }
}
