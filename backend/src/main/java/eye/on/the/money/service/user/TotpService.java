package eye.on.the.money.service.user;

import eye.on.the.money.dto.out.TotpSetupDTO;
import eye.on.the.money.exception.PasswordException;
import eye.on.the.money.exception.TotpException;
import eye.on.the.money.model.User;
import eye.on.the.money.model.user.UserTotp;
import eye.on.the.money.repository.user.UserTotpRepository;
import eye.on.the.money.security.TotpSecretEncryptor;
import eye.on.the.money.util.LogSanitizer;
import eye.on.the.money.util.QrCodes;
import eye.on.the.money.util.TotpCodes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class TotpService {

    private static final String ISSUER = "EOTM";

    private static final int SKEW_STEPS = 1;

    private static final int QR_SIZE = 240;

    private final UserTotpRepository userTotpRepository;

    private final UserService userService;

    private final TotpSecretEncryptor encryptor;

    private final PasswordEncoder passwordEncoder;

    public boolean isEnabled(Long userId) {
        return this.userTotpRepository.existsByUserIdAndConfirmedTrue(userId);
    }

    public boolean isEnabled(String email) {
        return this.userTotpRepository.findByUserEmailAndConfirmedTrue(email).isPresent();
    }

    @Transactional
    public TotpSetupDTO startEnrolment(Long userId) {
        User user = this.userService.loadUserById(userId);
        UserTotp totp = this.userTotpRepository.findByUserId(userId).orElse(null);

        if (totp != null && totp.isConfirmed()) {
            throw new TotpException("Two-factor authentication is already enabled");
        }
        if (totp == null) {
            totp = UserTotp.builder().user(user).build();
        }

        String secret = TotpCodes.generateSecret();
        totp.setSecret(this.encryptor.encrypt(secret));
        totp.setConfirmed(false);
        totp.setConfirmedAt(null);
        totp.setLastTimeStep(null);
        this.userTotpRepository.save(totp);

        String uri = this.otpauthUri(user.getEmail(), secret);
        return new TotpSetupDTO(secret, uri, QrCodes.toPngDataUri(uri, QR_SIZE));
    }

    @Transactional
    public void confirmEnrolment(Long userId, String code) {
        UserTotp totp = this.userTotpRepository.findByUserId(userId)
                .orElseThrow(() -> new TotpException("Start the setup before confirming it"));

        if (totp.isConfirmed()) {
            throw new TotpException("Two-factor authentication is already enabled");
        }

        long step = this.matchingStep(totp, code);
        if (step == TotpCodes.NO_MATCH) {
            throw new TotpException("That code is not right");
        }

        totp.setConfirmed(true);
        totp.setConfirmedAt(LocalDateTime.now());
        totp.setLastTimeStep(step);
        log.info("Two-factor authentication enabled for {}", LogSanitizer.maskEmail(totp.getUser().getEmail()));
    }

    @Transactional
    public boolean verify(String email, String code) {
        UserTotp totp = this.userTotpRepository.findByUserEmailAndConfirmedTrue(email).orElse(null);
        if (totp == null) {
            return false;
        }

        long step = this.matchingStep(totp, code);
        if (step == TotpCodes.NO_MATCH || (totp.getLastTimeStep() != null && step <= totp.getLastTimeStep())) {
            log.warn("Two-factor check failed for {}", LogSanitizer.maskEmail(email));
            return false;
        }

        totp.setLastTimeStep(step);
        return true;
    }

    @Transactional
    public void disable(Long userId, String password) {
        User user = this.userService.loadUserById(userId);

        if (!this.passwordEncoder.matches(password, user.getPassword())) {
            log.info("Incorrect password provided while disabling two-factor for {}",
                    LogSanitizer.maskEmail(user.getEmail()));
            throw new PasswordException("Invalid password provided");
        }

        this.userTotpRepository.deleteByUserId(userId);
        log.info("Two-factor authentication disabled for {}", LogSanitizer.maskEmail(user.getEmail()));
    }

    private long matchingStep(UserTotp totp, String code) {
        String secret = this.encryptor.decrypt(totp.getSecret());
        long currentStep = TotpCodes.timeStep(Instant.now().getEpochSecond());
        return TotpCodes.matchingStep(secret, code, currentStep, SKEW_STEPS);
    }

    private String otpauthUri(String email, String secret) {
        String account = URLEncoder.encode(email, StandardCharsets.UTF_8).replace("+", "%20");
        return "otpauth://totp/" + ISSUER + ":" + account
                + "?secret=" + secret
                + "&issuer=" + ISSUER
                + "&algorithm=SHA1"
                + "&digits=" + TotpCodes.DIGITS
                + "&period=" + TotpCodes.PERIOD_SECONDS;
    }
}
