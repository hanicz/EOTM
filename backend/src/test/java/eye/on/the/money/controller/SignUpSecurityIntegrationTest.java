package eye.on.the.money.controller;

import eye.on.the.money.EotmApplication;
import eye.on.the.money.model.User;
import eye.on.the.money.repository.UserRepository;
import eye.on.the.money.service.user.UserService;
import eye.on.the.money.dto.in.SignUpDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = EotmApplication.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
public class SignUpSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    public void signUpCannotOverwriteAnotherUserById() throws Exception {
        this.userService.signUp(new SignUpDTO("idvictim@mail.com", "victimPassword"));
        Long victimId = this.userRepository.findByEmail("idvictim@mail.com").getId();

        this.mockMvc.perform(post("/api/v1/user/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":" + victimId + ",\"email\":\"idattacker@mail.com\",\"password\":\"attackerPassword\"}"))
                .andExpect(status().isBadRequest());

        User victim = this.userRepository.findById(victimId).orElseThrow();
        Assertions.assertEquals("idvictim@mail.com", victim.getEmail());
        Assertions.assertTrue(this.passwordEncoder.matches("victimPassword", victim.getPassword()));
    }

    @Test
    public void signUpRejectsDuplicateEmail() throws Exception {
        this.userService.signUp(new SignUpDTO("dupvictim@mail.com", "victimPassword"));

        this.mockMvc.perform(post("/api/v1/user/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"dupvictim@mail.com\",\"password\":\"attackerPassword\"}"))
                .andExpect(status().isConflict());

        Assertions.assertEquals(1, this.userRepository.findAll().stream()
                .filter(user -> "dupvictim@mail.com".equals(user.getEmail()))
                .count());
    }

    @Test
    public void signUpRejectsInvalidEmail() throws Exception {
        this.mockMvc.perform(post("/api/v1/user/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"notanemail\",\"password\":\"validPassword\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void signUpRejectsShortPassword() throws Exception {
        this.mockMvc.perform(post("/api/v1/user/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"shortpw@mail.com\",\"password\":\"short\"}"))
                .andExpect(status().isBadRequest());
    }
}
