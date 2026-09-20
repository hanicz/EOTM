package eye.on.the.money.service.user;

import eye.on.the.money.dto.out.TotpSetupDTO;
import eye.on.the.money.exception.PasswordException;
import eye.on.the.money.exception.TotpException;
import eye.on.the.money.model.User;
import eye.on.the.money.model.user.UserTotp;
import eye.on.the.money.repository.user.UserTotpRepository;
import eye.on.the.money.security.TotpSecretEncryptor;
import eye.on.the.money.util.TotpCodes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TotpServiceTest {

    private static final Long USER_ID = 7L;

    private static final String EMAIL = "totpuser@mail.com";

    private static final String SECRET = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ";

    @Mock
    private UserTotpRepository userTotpRepository;

    @Mock
    private UserService userService;

    @Mock
    private PasswordEncoder passwordEncoder;

    private TotpSecretEncryptor encryptor;

    private TotpService totpService;

    private final User user = User.builder().id(USER_ID).email(EMAIL).password("hashed").build();

    @BeforeEach
    public void setUp() {
        this.encryptor = new TotpSecretEncryptor("a-test-totp-key", "7b5d41c9a6e230f8");
        this.encryptor.init();
        this.totpService = new TotpService(this.userTotpRepository, this.userService, this.encryptor,
                this.passwordEncoder);
    }

    private UserTotp enrolled(boolean confirmed) {
        return UserTotp.builder()
                .id(1L)
                .user(this.user)
                .secret(this.encryptor.encrypt(SECRET))
                .confirmed(confirmed)
                .build();
    }

    private String currentCode() {
        return TotpCodes.codeAt(SECRET, TotpCodes.timeStep(Instant.now().getEpochSecond()));
    }

    @Test
    public void startEnrolmentStoresAnUnconfirmedSecret() {
        when(this.userService.loadUserById(USER_ID)).thenReturn(this.user);
        when(this.userTotpRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        TotpSetupDTO setup = this.totpService.startEnrolment(USER_ID);

        ArgumentCaptor<UserTotp> saved = ArgumentCaptor.forClass(UserTotp.class);
        verify(this.userTotpRepository).save(saved.capture());
        assertFalse(saved.getValue().isConfirmed());
        assertNull(saved.getValue().getLastTimeStep());
        assertEquals(setup.secret(), this.encryptor.decrypt(saved.getValue().getSecret()));
        assertEquals(32, setup.secret().length());
    }

    @Test
    public void startEnrolmentBuildsAnAuthenticatorUri() {
        when(this.userService.loadUserById(USER_ID)).thenReturn(this.user);
        when(this.userTotpRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        TotpSetupDTO setup = this.totpService.startEnrolment(USER_ID);

        assertTrue(setup.otpauthUri().startsWith("otpauth://totp/EOTM:"));
        assertTrue(setup.otpauthUri().contains("secret=" + setup.secret()));
        assertTrue(setup.otpauthUri().contains("issuer=EOTM"));
        assertTrue(setup.otpauthUri().contains("algorithm=SHA1"));
        assertTrue(setup.otpauthUri().contains("digits=6"));
        assertTrue(setup.otpauthUri().contains("period=30"));
        assertTrue(setup.otpauthUri().contains("totpuser%40mail.com"));
        assertTrue(setup.qrPng().startsWith("data:image/png;base64,"));
    }

    @Test
    public void startEnrolmentReplacesAPendingSecret() {
        UserTotp pending = this.enrolled(false);
        when(this.userService.loadUserById(USER_ID)).thenReturn(this.user);
        when(this.userTotpRepository.findByUserId(USER_ID)).thenReturn(Optional.of(pending));

        TotpSetupDTO setup = this.totpService.startEnrolment(USER_ID);

        assertEquals(setup.secret(), this.encryptor.decrypt(pending.getSecret()));
        verify(this.userTotpRepository).save(pending);
    }

    @Test
    public void startEnrolmentRefusesWhenAlreadyEnabled() {
        when(this.userService.loadUserById(USER_ID)).thenReturn(this.user);
        when(this.userTotpRepository.findByUserId(USER_ID)).thenReturn(Optional.of(this.enrolled(true)));

        assertThrows(TotpException.class, () -> this.totpService.startEnrolment(USER_ID));
        verify(this.userTotpRepository, never()).save(ArgumentMatchers.any());
    }

    @Test
    public void confirmEnrolmentEnablesOnACorrectCode() {
        UserTotp pending = this.enrolled(false);
        when(this.userTotpRepository.findByUserId(USER_ID)).thenReturn(Optional.of(pending));

        this.totpService.confirmEnrolment(USER_ID, this.currentCode());

        assertTrue(pending.isConfirmed());
        assertNotNull(pending.getConfirmedAt());
        assertNotNull(pending.getLastTimeStep());
    }

    @Test
    public void confirmEnrolmentRejectsAWrongCode() {
        UserTotp pending = this.enrolled(false);
        when(this.userTotpRepository.findByUserId(USER_ID)).thenReturn(Optional.of(pending));

        assertThrows(TotpException.class, () -> this.totpService.confirmEnrolment(USER_ID, "000000"));
        assertFalse(pending.isConfirmed());
    }

    @Test
    public void confirmEnrolmentNeedsASetupFirst() {
        when(this.userTotpRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        assertThrows(TotpException.class, () -> this.totpService.confirmEnrolment(USER_ID, "000000"));
    }

    @Test
    public void confirmEnrolmentRefusesWhenAlreadyEnabled() {
        when(this.userTotpRepository.findByUserId(USER_ID)).thenReturn(Optional.of(this.enrolled(true)));

        assertThrows(TotpException.class, () -> this.totpService.confirmEnrolment(USER_ID, this.currentCode()));
    }

    @Test
    public void verifyAcceptsAFreshCodeAndRecordsTheStep() {
        UserTotp totp = this.enrolled(true);
        when(this.userTotpRepository.findByUserEmailAndConfirmedTrue(EMAIL)).thenReturn(Optional.of(totp));

        assertTrue(this.totpService.verify(EMAIL, this.currentCode()));
        assertEquals(TotpCodes.timeStep(Instant.now().getEpochSecond()), totp.getLastTimeStep());
    }

    @Test
    public void verifyAcceptsTheNeighbouringStep() {
        UserTotp totp = this.enrolled(true);
        when(this.userTotpRepository.findByUserEmailAndConfirmedTrue(EMAIL)).thenReturn(Optional.of(totp));
        long previous = TotpCodes.timeStep(Instant.now().getEpochSecond()) - 1;

        assertTrue(this.totpService.verify(EMAIL, TotpCodes.codeAt(SECRET, previous)));
    }

    @Test
    public void verifyRejectsAReplayedCode() {
        UserTotp totp = this.enrolled(true);
        when(this.userTotpRepository.findByUserEmailAndConfirmedTrue(EMAIL)).thenReturn(Optional.of(totp));
        String code = this.currentCode();

        assertTrue(this.totpService.verify(EMAIL, code));
        assertFalse(this.totpService.verify(EMAIL, code));
    }

    @Test
    public void verifyRejectsAWrongCode() {
        when(this.userTotpRepository.findByUserEmailAndConfirmedTrue(EMAIL))
                .thenReturn(Optional.of(this.enrolled(true)));

        assertFalse(this.totpService.verify(EMAIL, "000000"));
    }

    @Test
    public void verifyRejectsWhenNothingIsEnrolled() {
        when(this.userTotpRepository.findByUserEmailAndConfirmedTrue(EMAIL)).thenReturn(Optional.empty());

        assertFalse(this.totpService.verify(EMAIL, this.currentCode()));
    }

    @Test
    public void isEnabledFollowsTheConfirmedFlag() {
        when(this.userTotpRepository.existsByUserIdAndConfirmedTrue(USER_ID)).thenReturn(true);
        when(this.userTotpRepository.findByUserEmailAndConfirmedTrue(EMAIL)).thenReturn(Optional.empty());

        assertTrue(this.totpService.isEnabled(USER_ID));
        assertFalse(this.totpService.isEnabled(EMAIL));
    }

    @Test
    public void disableNeedsTheCurrentPassword() {
        when(this.userService.loadUserById(USER_ID)).thenReturn(this.user);
        when(this.passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThrows(PasswordException.class, () -> this.totpService.disable(USER_ID, "wrong"));
        verify(this.userTotpRepository, never()).deleteByUserId(USER_ID);
    }

    @Test
    public void disableRemovesTheEnrolment() {
        when(this.userService.loadUserById(USER_ID)).thenReturn(this.user);
        when(this.passwordEncoder.matches("right", "hashed")).thenReturn(true);

        this.totpService.disable(USER_ID, "right");

        verify(this.userTotpRepository).deleteByUserId(USER_ID);
    }
}
