package eye.on.the.money.controller;

import eye.on.the.money.EotmApplication;
import eye.on.the.money.dto.in.SignUpDTO;
import eye.on.the.money.dto.out.TotpSetupDTO;
import eye.on.the.money.model.User;
import eye.on.the.money.security.JwtService;
import eye.on.the.money.service.user.TotpService;
import eye.on.the.money.service.user.UserService;
import eye.on.the.money.util.TotpCodes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = EotmApplication.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class TwoFactorSecurityIntegrationTest {

    private static final String PASSWORD = "correctPassword";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Autowired
    private TotpService totpService;

    @Autowired
    private JwtService jwtService;

    private String enrol(String email) {
        this.userService.signUp(new SignUpDTO(email, PASSWORD));
        User user = this.userService.loadUserByEmail(email);
        TotpSetupDTO setup = this.totpService.startEnrolment(user.getId());
        this.totpService.confirmEnrolment(user.getId(), codeFor(setup.secret(), 0));
        return setup.secret();
    }

    private static String codeFor(String secret, int stepOffset) {
        return TotpCodes.codeAt(secret, TotpCodes.timeStep(Instant.now().getEpochSecond()) + stepOffset);
    }

    private MockHttpServletResponse login(String email) throws Exception {
        return this.mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andReturn().getResponse();
    }

    private MockHttpServletResponse verify(String mfaToken, String code) throws Exception {
        return this.mockMvc.perform(post("/api/v1/auth/2fa/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mfaToken\":\"" + mfaToken + "\",\"code\":\"" + code + "\"}"))
                .andReturn().getResponse();
    }

    @Test
    public void loginWithoutTwoFactorStillIssuesATokenDirectly() throws Exception {
        this.userService.signUp(new SignUpDTO("nofactor@mail.com", PASSWORD));

        MockHttpServletResponse response = this.login("nofactor@mail.com");

        assertNotNull(response.getHeader("token"));
        assertTrue(response.getContentAsString().contains("\"mfaRequired\":false"));
    }

    @Test
    public void loginWithTwoFactorReturnsAChallengeInsteadOfAToken() throws Exception {
        this.enrol("challenge@mail.com");

        MockHttpServletResponse response = this.login("challenge@mail.com");
        String body = response.getContentAsString();

        assertNull(response.getHeader("token"), "login issued an access token while two-factor was enabled");
        assertTrue(body.contains("\"mfaRequired\":true"), body);
        assertTrue(body.contains("\"mfaToken\":\""), body);
    }

    @Test
    public void aChallengeTokenDoesNotAuthoriseApiCalls() throws Exception {
        this.userService.signUp(new SignUpDTO("notanaccess@mail.com", PASSWORD));
        String challenge = this.jwtService.generateChallengeToken("notanaccess@mail.com");

        this.mockMvc.perform(get("/api/v1/user/me").header("Authorization", challenge))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.error").value("Authorization failed"));
    }

    @Test
    public void anAccessTokenStillAuthorisesApiCalls() throws Exception {
        this.userService.signUp(new SignUpDTO("stillworks@mail.com", PASSWORD));
        String token = this.jwtService.generateAccessToken("stillworks@mail.com");

        this.mockMvc.perform(get("/api/v1/user/me").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("stillworks@mail.com"));
    }

    @Test
    public void verifyingTheCodeIssuesTheAccessToken() throws Exception {
        String secret = this.enrol("verifyme@mail.com");
        String challenge = this.jwtService.generateChallengeToken("verifyme@mail.com");

        MockHttpServletResponse response = this.verify(challenge, codeFor(secret, 1));

        org.junit.jupiter.api.Assertions.assertEquals(204, response.getStatus(), response.getContentAsString());
        assertNotNull(response.getHeader("token"));
    }

    @Test
    public void aCodeCannotBeUsedTwice() throws Exception {
        String secret = this.enrol("replay@mail.com");
        String challenge = this.jwtService.generateChallengeToken("replay@mail.com");
        String code = codeFor(secret, 1);

        org.junit.jupiter.api.Assertions.assertEquals(204, this.verify(challenge, code).getStatus());
        org.junit.jupiter.api.Assertions.assertEquals(401, this.verify(challenge, code).getStatus());
    }

    @Test
    public void aWrongCodeIsRejected() throws Exception {
        this.enrol("wrongcode@mail.com");
        String challenge = this.jwtService.generateChallengeToken("wrongcode@mail.com");

        this.mockMvc.perform(post("/api/v1/auth/2fa/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mfaToken\":\"" + challenge + "\",\"code\":\"000000\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    public void anAccessTokenCannotStandInForTheChallenge() throws Exception {
        String secret = this.enrol("notachallenge@mail.com");
        String access = this.jwtService.generateAccessToken("notachallenge@mail.com");

        org.junit.jupiter.api.Assertions.assertEquals(401, this.verify(access, codeFor(secret, 1)).getStatus());
    }

    @Test
    public void aGarbageChallengeIsRejected() throws Exception {
        org.junit.jupiter.api.Assertions.assertEquals(401, this.verify("not.a.token", "000000").getStatus());
    }

    @Test
    public void theVerifyEndpointNeedsBothFields() throws Exception {
        this.mockMvc.perform(post("/api/v1/auth/2fa/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"000000\"}"))
                .andExpect(status().isBadRequest());
    }
}
