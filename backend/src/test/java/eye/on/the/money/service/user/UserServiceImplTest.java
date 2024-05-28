package eye.on.the.money.service.user;

import eye.on.the.money.EotmApplication;
import eye.on.the.money.dto.in.ChangePasswordDTO;
import eye.on.the.money.exception.PasswordException;
import eye.on.the.money.model.User;
import eye.on.the.money.repository.UserRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@SpringBootTest(classes = EotmApplication.class)
@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
class UserServiceImplTest {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserServiceImpl userService;


    @Test
    public void signUp() {
        User user = User.builder().email("newuser@mail.com").password("testPassword").build();
        this.userService.signUp(user);

        Assertions.assertEquals(user.getEmail(), this.userRepository.findByEmail("newuser@mail.com").getEmail());
    }

    @Test
    public void loadUserByUsername() {
        User user = User.builder().email("loaduser@mail.com").password("testPassword").build();
        this.userRepository.save(user);
        Assertions.assertEquals(user.getEmail(), this.userService.loadUserByUsername("loaduser@mail.com").getUsername());
    }

    @Test
    public void loadUserByUsernameNotFound() {
        Assertions.assertThrows(UsernameNotFoundException.class, () -> {
            this.userService.loadUserByUsername("notexists@mail.com").getUsername();
        });
    }

    @Test
    public void loadUserByEmail() {
        User user = User.builder().email("loaduser2@mail.com").password("testPassword").build();
        this.userRepository.save(user);
        Assertions.assertEquals(user.getEmail(), this.userService.loadUserByEmail("loaduser2@mail.com").getUsername());
    }

    @Test
    public void loadUserByEmailNotFound() {
        Assertions.assertThrows(UsernameNotFoundException.class, () -> {
            this.userService.loadUserByEmail("notexists@mail.com").getUsername();
        });
    }

    @Test
    public void changePassword() {
        User user = User.builder().email("changePassword@mail.com").password("testPassword").build();
        this.userService.signUp(user);
        this.userService.changePassword("changePassword@mail.com", new ChangePasswordDTO("newPassword", "testPassword"));

        Assertions.assertTrue(this.passwordEncoder.matches("newPassword", this.userRepository.findByEmail("changePassword@mail.com").getPassword()));
    }

    @Test
    public void changePasswordIncorrectOldPassword() {
       Assertions.assertThrows(PasswordException.class, () -> {
           this.userService.changePassword("test@test.test", new ChangePasswordDTO("newPassword", "incorrectOldPassword"));
       });
    }
}