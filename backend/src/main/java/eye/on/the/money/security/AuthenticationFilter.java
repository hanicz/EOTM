package eye.on.the.money.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import eye.on.the.money.exception.dto.ErrorResponse;
import eye.on.the.money.util.LogSanitizer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
@RequiredArgsConstructor
public class AuthenticationFilter extends UsernamePasswordAuthenticationFilter {

    private static final String BAD_CREDENTIALS_MESSAGE = "Invalid email or password";

    private static final String MALFORMED_BODY_MESSAGE = "Malformed request body";

    private static final String ATTEMPTED_EMAIL = "eotm.attemptedEmail";

    private final AuthenticationManager authenticationManager;

    private final JwtService jwtService;

    private final ObjectMapper objectMapper;

    @Override
    protected void successfulAuthentication(HttpServletRequest req, HttpServletResponse res, FilterChain chain,
                                            Authentication auth) {
        String email = ((UserDetails) auth.getPrincipal()).getUsername();
        log.info("Login succeeded for {} from {}", LogSanitizer.maskEmail(email), req.getRemoteAddr());
        res.addHeader("token", this.jwtService.generateToken(email));
    }

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest req, HttpServletResponse res,
                                              AuthenticationException failed) throws IOException {
        SecurityContextHolder.clearContext();
        String email = LogSanitizer.maskEmail((String) req.getAttribute(ATTEMPTED_EMAIL));
        if (failed instanceof MalformedLoginRequestException) {
            log.warn("Login rejected, malformed body, from {}: {}", req.getRemoteAddr(), failed.getMessage());
            this.writeError(res, HttpServletResponse.SC_BAD_REQUEST, MALFORMED_BODY_MESSAGE);
            return;
        }
        log.warn("Login failed for {} from {}: {}", email, req.getRemoteAddr(), failed.getMessage());
        this.writeError(res, HttpServletResponse.SC_UNAUTHORIZED, BAD_CREDENTIALS_MESSAGE);
    }

    private void writeError(HttpServletResponse res, int status, String message) throws IOException {
        res.setStatus(status);
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding(StandardCharsets.UTF_8.name());
        this.objectMapper.writeValue(res.getWriter(), new ErrorResponse(status, message));
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest req,
                                                HttpServletResponse res) throws AuthenticationException {
        eye.on.the.money.model.User user;
        try {
            user = this.objectMapper.readValue(req.getInputStream(), eye.on.the.money.model.User.class);
        } catch (IOException e) {
            throw new MalformedLoginRequestException(MALFORMED_BODY_MESSAGE, e);
        }

        req.setAttribute(ATTEMPTED_EMAIL, user.getEmail());
        return authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(user.getEmail(), user.getPassword()));
    }
}
