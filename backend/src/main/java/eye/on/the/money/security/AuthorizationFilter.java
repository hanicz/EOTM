package eye.on.the.money.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import eye.on.the.money.exception.dto.ErrorResponse;
import eye.on.the.money.logging.RequestLoggingFilter;
import eye.on.the.money.model.User;
import eye.on.the.money.service.user.UserService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import static eye.on.the.money.security.SecurityConstants.HEADER_NAME;

@Slf4j
@RequiredArgsConstructor
public class AuthorizationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_FAILED_MESSAGE = "Authorization failed";

    private final UserService userService;
    private final JwtService jwtService;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String token = request.getHeader(HEADER_NAME);

        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }
        UserDetails userDetails;
        try {
            String subject = this.jwtService.extractUsername(token);
            userDetails = this.userService.loadUserByUsername(subject);
        } catch (JwtException | IllegalArgumentException | UsernameNotFoundException e) {
            log.warn("Auth failed for {} {}: {}", request.getMethod(), request.getRequestURI(), e.getMessage());
            this.writeError(response, HttpServletResponse.SC_UNAUTHORIZED, AUTHORIZATION_FAILED_MESSAGE);
            return;
        }

        if (userDetails instanceof User user) {
            MDC.put(RequestLoggingFilter.USER_ID, String.valueOf(user.getId()));
        }

        UsernamePasswordAuthenticationToken upa =
                new UsernamePasswordAuthenticationToken(userDetails, null, new ArrayList<>());
        SecurityContextHolder.getContext().setAuthentication(upa);
        filterChain.doFilter(request, response);
    }

    private void writeError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        this.objectMapper.writeValue(response.getWriter(), new ErrorResponse(status, message));
    }
}
