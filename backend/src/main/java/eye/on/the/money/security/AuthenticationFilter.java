package eye.on.the.money.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eye.on.the.money.exception.dto.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
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

@RequiredArgsConstructor
public class AuthenticationFilter extends UsernamePasswordAuthenticationFilter {

    private static final String BAD_CREDENTIALS_MESSAGE = "Invalid email or password";

    private static final String MALFORMED_BODY_MESSAGE = "Malformed request body";

    private final AuthenticationManager authenticationManager;

    private final JwtService jwtService;

    private final ObjectMapper objectMapper;

    @Override
    protected void successfulAuthentication(HttpServletRequest req, HttpServletResponse res, FilterChain chain,
                                            Authentication auth) {
        res.addHeader("token", this.jwtService.generateToken(((UserDetails) auth.getPrincipal()).getUsername()));
    }

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest req, HttpServletResponse res,
                                              AuthenticationException failed) throws IOException {
        SecurityContextHolder.clearContext();
        if (failed instanceof MalformedLoginRequestException) {
            this.writeError(res, HttpServletResponse.SC_BAD_REQUEST, MALFORMED_BODY_MESSAGE);
            return;
        }
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
        } catch (JsonProcessingException e) {
            throw new MalformedLoginRequestException(MALFORMED_BODY_MESSAGE, e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(user.getEmail(), user.getPassword()));
    }
}
