package eye.on.the.money.controller;

import eye.on.the.money.EotmApplication;
import eye.on.the.money.dto.in.SignUpDTO;
import eye.on.the.money.service.user.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = EotmApplication.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class LoginSecurityIntegrationTest {

    private static final String GENERIC_MESSAGE = "Invalid email or password";

    private static final String MALFORMED_MESSAGE = "Malformed request body";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Test
    public void unknownUserGetsJsonErrorResponse() throws Exception {
        this.mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nosuchuser@mail.com\",\"password\":\"whatever\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.error").value(GENERIC_MESSAGE));
    }

    @Test
    public void wrongPasswordIsIndistinguishableFromUnknownUser() throws Exception {
        this.userService.signUp(new SignUpDTO("loginexisting@mail.com", "correctPassword"));

        this.mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"loginexisting@mail.com\",\"password\":\"wrongPassword\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.error").value(GENERIC_MESSAGE));
    }

    @Test
    public void malformedJsonBodyReturnsBadRequest() throws Exception {
        this.mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.error").value(MALFORMED_MESSAGE));
    }

    @Test
    public void emptyBodyReturnsBadRequest() throws Exception {
        this.mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.error").value(MALFORMED_MESSAGE));
    }

    @Test
    public void unknownFieldInBodyReturnsBadRequest() throws Exception {
        this.mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bogusField\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }
}
