package eye.on.the.money.security;

import eye.on.the.money.service.user.UserServiceImpl;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import static eye.on.the.money.security.SecurityConstants.HEADER_NAME;

@Slf4j
@RequiredArgsConstructor
public class AuthorizationFilter extends OncePerRequestFilter {

    private final UserServiceImpl userService;
    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String token = request.getHeader(HEADER_NAME);

        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }
        try {
            String subject = this.jwtService.extractUsername(token);
            UsernamePasswordAuthenticationToken upa =
                    new UsernamePasswordAuthenticationToken(this.userService.loadUserByUsername(subject), null, new ArrayList<>());
            SecurityContextHolder.getContext().setAuthentication(upa);
            filterChain.doFilter(request, response);
        } catch (SignatureException | ExpiredJwtException e) {
            log.warn("Auth failed: {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            PrintWriter pw = response.getWriter();
            pw.write("HTTP Status 401 - Authorization failed");
        }
    }
}
