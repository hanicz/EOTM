package eye.on.the.money.security;

import eye.on.the.money.model.User;
import eye.on.the.money.service.impl.UserServiceImpl;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;

import static eye.on.the.money.util.SecurityConstants.HEADER_NAME;
import static eye.on.the.money.util.SecurityConstants.KEY;

public class AuthorizationFilter extends BasicAuthenticationFilter {

    private final UserServiceImpl userService;

    public AuthorizationFilter(AuthenticationManager authManager, UserServiceImpl userService) {
        super(authManager);
        this.userService = userService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws IOException, ServletException {
        String header = request.getHeader(HEADER_NAME);

        if (header == null) {
            chain.doFilter(request, response);
            return;
        }

        UsernamePasswordAuthenticationToken authentication = authenticate(request);

        SecurityContextHolder.getContext().setAuthentication(authentication);
        chain.doFilter(request, response);
    }

    private UsernamePasswordAuthenticationToken authenticate(HttpServletRequest request) {
        String token = request.getHeader(HEADER_NAME);
        if (token != null) {
            String user = Jwts.parser()
                    .setSigningKey(Keys.hmacShaKeyFor(KEY.getBytes()))
                    .parseClaimsJws(token)
                    .getBody()
                    .getSubject();

            if (user != null) {
                User userInDb = this.userService.loadUserByEmail(user);
                return new UsernamePasswordAuthenticationToken(userInDb, null, new ArrayList<>());
            } else {
                return null;
            }

        }
        return null;
    }
}
