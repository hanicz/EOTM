package eye.on.the.money.security;

import eye.on.the.money.service.UserServiceImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;

import static eye.on.the.money.security.SecurityConstants.HEADER_NAME;

public class AuthorizationFilter extends OncePerRequestFilter {

    private final UserServiceImpl userService;
    private final JwtService jwtService;

    public AuthorizationFilter(UserServiceImpl userService, JwtService jwtService) {
        this.userService = userService;
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String token = request.getHeader(HEADER_NAME);

        if(token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String subject = this.jwtService.extractUsername(token);

        UsernamePasswordAuthenticationToken upa =
                new UsernamePasswordAuthenticationToken(this.userService.loadUserByUsername(subject), null, new ArrayList<>());
        SecurityContextHolder.getContext().setAuthentication(upa);
        filterChain.doFilter(request,response);
    }
}
