package eye.on.the.money.service.user;

import eye.on.the.money.EotmApplication;
import eye.on.the.money.dto.in.ChangePasswordDTO;
import eye.on.the.money.dto.in.SignUpDTO;
import eye.on.the.money.exception.PasswordException;
import eye.on.the.money.exception.UserAlreadyExistsException;
import eye.on.the.money.model.User;
import eye.on.the.money.repository.UserRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@SpringBootTest(classes = EotmApplication.class)
@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
class UserServiceTest {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;


    @Test
    public void signUp() {
        this.userService.signUp(new SignUpDTO("newuser@mail.com", "testPassword"));

        User created = this.userRepository.findByEmail("newuser@mail.com");
        Assertions.assertEquals("newuser@mail.com", created.getEmail());
        Assertions.assertTrue(this.passwordEncoder.matches("testPassword", created.getPassword()));
    }

    @Test
    public void signUpRejectsExistingEmail() {
        this.userService.signUp(new SignUpDTO("duplicate@mail.com", "testPassword"));

        Assertions.assertThrows(UserAlreadyExistsException.class, () ->
                this.userService.signUp(new SignUpDTO("duplicate@mail.com", "otherPassword")));
    }

    @Test
    public void signUpRejectsExistingEmailIgnoringCase() {
        this.userService.signUp(new SignUpDTO("casing@mail.com", "testPassword"));

        Assertions.assertThrows(UserAlreadyExistsException.class, () ->
                this.userService.signUp(new SignUpDTO("CaSiNg@mail.com", "otherPassword")));
    }

    @Test
    public void signUpDoesNotOverwriteAnExistingUser() {
        this.userService.signUp(new SignUpDTO("victim@mail.com", "victimPassword"));
        Long victimId = this.userRepository.findByEmail("victim@mail.com").getId();

        this.userService.signUp(new SignUpDTO("attacker@mail.com", "attackerPassword"));

        User victim = this.userRepository.findById(victimId).orElseThrow();
        Assertions.assertEquals("victim@mail.com", victim.getEmail());
        Assertions.assertTrue(this.passwordEncoder.matches("victimPassword", victim.getPassword()));
    }

    @Test
    public void loadUserByUsername() {
        User user = User.builder().email("loaduser@mail.com").password("testPassword").build();
        this.userRepository.save(user);
        UserDetails loaded = this.userService.loadUserByUsername("loaduser@mail.com");
        Assertions.assertAll("The principal carries the id and authenticates",
                () -> Assertions.assertEquals(user.getEmail(), loaded.getUsername()),
                () -> Assertions.assertEquals(user.getId(), ((User) loaded).getId()),
                () -> Assertions.assertTrue(loaded.isEnabled()));
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
        this.userService.signUp(new SignUpDTO("changePassword@mail.com", "testPassword"));
        Long userId = this.userRepository.findByEmail("changePassword@mail.com").getId();
        this.userService.changePassword(userId, new ChangePasswordDTO("newPassword", "testPassword"));

        Assertions.assertTrue(this.passwordEncoder.matches("newPassword", this.userRepository.findByEmail("changePassword@mail.com").getPassword()));
    }

    @Test
    public void changePasswordIncorrectOldPassword() {
       Assertions.assertThrows(PasswordException.class, () -> {
           Long userId = this.userRepository.findByEmail("test@test.test").getId();
           this.userService.changePassword(userId, new ChangePasswordDTO("newPassword", "incorrectOldPassword"));
       });
    }
}