package eye.on.the.money.service.user;

import eye.on.the.money.dto.in.ChangePasswordDTO;
import eye.on.the.money.dto.in.SignUpDTO;
import eye.on.the.money.dto.out.UserDTO;
import eye.on.the.money.exception.PasswordException;
import eye.on.the.money.exception.UserAlreadyExistsException;
import eye.on.the.money.model.User;
import eye.on.the.money.repository.UserRepository;
import eye.on.the.money.repository.forex.CurrencyRepository;
import eye.on.the.money.util.LogSanitizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService implements UserDetailsService {
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final CurrencyRepository currencyRepository;

    public void signUp(SignUpDTO signUpDTO) {
        if (this.userRepository.existsByEmailIgnoreCase(signUpDTO.email())) {
            log.info("Sign up rejected, email already registered: {}", LogSanitizer.maskEmail(signUpDTO.email()));
            throw new UserAlreadyExistsException("An account already exists for this email address");
        }

        User user = User.builder()
                .email(signUpDTO.email())
                .password(this.passwordEncoder.encode(signUpDTO.password()))
                .build();

        try {
            this.userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            log.info("Sign up rejected, email already registered: {}", LogSanitizer.maskEmail(signUpDTO.email()));
            throw new UserAlreadyExistsException("An account already exists for this email address", e);
        }
        log.info("User created: {}", LogSanitizer.maskEmail(user.getEmail()));
    }

    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return this.loadUserByEmail(email);
    }

    public User getReference(Long id) {
        return this.userRepository.getReferenceById(id);
    }

    public User loadUserById(Long id) throws UsernameNotFoundException {
        return this.userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException(String.valueOf(id)));
    }

    public User loadUserByEmail(String email) throws UsernameNotFoundException {
        User user = this.userRepository.findByEmail(email);
        if (user == null) {
            throw new UsernameNotFoundException(email);
        }
        return user;
    }

    public UserDTO getUser(Long userId) {
        return this.toDTO(this.loadUserById(userId));
    }

    @Transactional
    public UserDTO updatePreferredCurrency(Long userId, String currencyId) {
        User user = this.loadUserById(userId);
        user.setPreferredCurrency(this.resolveCurrency(currencyId));
        return this.toDTO(user);
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordDTO passwordDTO) {
        User user = this.loadUserById(userId);

        if(this.passwordEncoder.matches(passwordDTO.oldPassword(), user.getPassword())) {
            user.setPassword(this.passwordEncoder.encode(passwordDTO.newPassword()));
            log.info("Password changed for user: {}", LogSanitizer.maskEmail(user.getEmail()));
        } else {
            log.info("Incorrect old password provided while changing password for user: {}",
                    LogSanitizer.maskEmail(user.getEmail()));
            throw new PasswordException("Invalid old password provided");
        }
    }

    private String resolveCurrency(String currencyId) {
        String id = (currencyId == null) ? "" : currencyId.trim().toUpperCase();
        return this.currencyRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Currency not found: " + id))
                .getId();
    }

    private UserDTO toDTO(User user) {
        String currency = user.getPreferredCurrency();
        return UserDTO.builder()
                .email(user.getEmail())
                .preferredCurrency((currency == null) ? User.DEFAULT_CURRENCY : currency)
                .build();
    }
}
