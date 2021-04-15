package eye.on.the.money.service.impl;

import eye.on.the.money.EotmApplication;
import eye.on.the.money.model.User;
import eye.on.the.money.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import static java.util.Collections.emptyList;

@Service
public class UserServiceImpl implements UserDetailsService {
    private static final Logger log = LoggerFactory.getLogger(EotmApplication.class);

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserRepository userRepository;


    public void signUp(User user) {
        user.setPassword(this.passwordEncoder.encode(user.getPassword()));
        this.userRepository.save(user);
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
}
