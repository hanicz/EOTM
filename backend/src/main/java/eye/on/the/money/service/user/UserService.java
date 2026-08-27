package eye.on.the.money.service.user;

import eye.on.the.money.dto.in.ChangePasswordDTO;
import eye.on.the.money.dto.in.SignUpDTO;
import eye.on.the.money.exception.PasswordException;
import eye.on.the.money.exception.UserAlreadyExistsException;
import eye.on.the.money.model.User;
import eye.on.the.money.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService implements UserDetailsService {
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    public void signUp(SignUpDTO signUpDTO) {
        if (this.userRepository.existsByEmailIgnoreCase(signUpDTO.email())) {
            log.info("Sign up rejected, email already registered: {}", signUpDTO.email());
            throw new UserAlreadyExistsException("An account already exists for this email address");
        }

        User user = User.builder()
                .email(signUpDTO.email())
                .password(this.passwordEncoder.encode(signUpDTO.password()))
                .build();

        try {
            this.userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            log.info("Sign up rejected, email already registered: {}", signUpDTO.email());
            throw new UserAlreadyExistsException("An account already exists for this email address", e);
        }
        log.info("User created: {}", user.getEmail());
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

    @Transactional
    public void changePassword(Long userId, ChangePasswordDTO passwordDTO) {
        User user = this.loadUserById(userId);

        if(this.passwordEncoder.matches(passwordDTO.oldPassword(), user.getPassword())) {
            user.setPassword(this.passwordEncoder.encode(passwordDTO.newPassword()));
            log.info("Password changed for user: {}", user.getEmail());
        } else {
            log.info("Incorrect old password provided while changing password for user: {}", user.getEmail());
            throw new PasswordException("Invalid old password provided");
        }
    }
}
