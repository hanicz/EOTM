package eye.on.the.money.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

@Slf4j
public class RequestLoggingFilter extends OncePerRequestFilter {

    public static final String TRACE_ID = "traceId";

    public static final String USER_ID = "userId";

    public static final String METHOD = "method";

    public static final String PATH = "path";

    public static final String REQUEST_ID_HEADER = "X-Request-Id";

    private static final int TRACE_ID_LENGTH = 12;

    private static final Pattern VALID_TRACE_ID = Pattern.compile("[A-Za-z0-9._-]{1,64}");

    private static final String[] IGNORED_PREFIXES = {"/resources/", "/actuator/"};

    private static final String[] IGNORED_PATHS = {"/", "/index.html", "/favicon.ico"};

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String traceId = this.resolveTraceId(request);
        MDC.put(TRACE_ID, traceId);
        MDC.put(METHOD, request.getMethod());
        MDC.put(PATH, request.getRequestURI());
        response.setHeader(REQUEST_ID_HEADER, traceId);
        long start = System.nanoTime();
        try {
            filterChain.doFilter(request, response);
        } finally {
            log.info("{} {} -> {} in {} ms", request.getMethod(), request.getRequestURI(), response.getStatus(),
                    (System.nanoTime() - start) / 1_000_000);
            MDC.clear();
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        for (String path : IGNORED_PATHS) {
            if (path.equals(uri)) {
                return true;
            }
        }
        for (String prefix : IGNORED_PREFIXES) {
            if (uri.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private String resolveTraceId(HttpServletRequest request) {
        String provided = request.getHeader(REQUEST_ID_HEADER);
        if (provided != null && VALID_TRACE_ID.matcher(provided).matches()) {
            return provided;
        }
        return UUID.randomUUID().toString().replace("-", "").substring(0, TRACE_ID_LENGTH);
    }
}
