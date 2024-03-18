package eye.on.the.money.service.user;

import eye.on.the.money.dto.in.ChangePasswordDTO;
import eye.on.the.money.model.User;
import eye.on.the.money.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import static java.util.Collections.emptyList;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl implements UserDetailsService {
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    public void signUp(User user) {
        user.setPassword(this.passwordEncoder.encode(user.getPassword()));
        this.userRepository.save(user);
        log.debug("User created: {}", user.getEmail());
    }

    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = this.userRepository.findByEmail(email);
        if (user == null) {
            throw new UsernameNotFoundException(email);
        }
        return new org.springframework.security.core.userdetails.User(user.getEmail(), user.getPassword(), emptyList());
    }

    public User loadUserByEmail(String email) throws UsernameNotFoundException {

        User user = this.userRepository.findByEmail(email);
        if (user == null) {
            throw new UsernameNotFoundException(email);
        }
        return user;
    }

    public void changePassword(String userEmail, ChangePasswordDTO passwordDTO) {
        User user = this.loadUserByEmail(userEmail);
        user.setPassword(this.passwordEncoder.encode(passwordDTO.password()));
        log.debug("Password changed for user: {}", user.getEmail());
    }
}
