package rw.bnr.user_service.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import rw.bnr.user_service.dto.ErrorResponse;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Slf4j
public class InternalAuthFilter extends OncePerRequestFilter {
    private final List<String> publicPaths = Arrays.asList(
            "/user/register",
            "/user/login",
            "/swagger-ui/",
            "/favicon.ico",
            "/v3/api-docs",
            "/swagger-resources",
            "/webjars/"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        log.debug("InternalAuthFilter - doFilterInternal");
        String path = request.getRequestURI();

        // Allow public endpoints without authentication
        if (isPublicPath(path)) {
            log.debug("InternalAuthFilter - path {} is public", path);
            filterChain.doFilter(request, response);
            return;
        }

        // Check for the special header from API Gateway
        String internalAuth = request.getHeader("X-Internal-Auth");
        log.info("InternalAuthFilter - internalAuth {}", internalAuth);

        if ("true".equals(internalAuth)) {
            log.info("InternalAuthFilter - internalAuth {} is true", internalAuth);

            Authentication auth = new UsernamePasswordAuthenticationToken(
                    "internal-user",
                    null,
                    null
            );
            SecurityContextHolder.getContext().setAuthentication(auth);
            filterChain.doFilter(request, response);
            return;
        }

        log.info("InternalAuthFilter - internalAuth {} is false", internalAuth);
        // If headers aren't present, return unauthorized
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ErrorResponse error = new ErrorResponse("Unauthorized access detected.");
        response.getWriter().write(error.toString());
    }

    private boolean isPublicPath(String path) {
        // Check if the path starts with any of the public path prefixes
        for (String prefix : publicPaths) {
            if (path.startsWith(prefix)) {
                log.debug("InternalAuthFilter - Path {} starts with public prefix {}", path, prefix);
                return true;
            }
        }

        // Additional checks for specific paths
        boolean isSwaggerUI = path.equals("/swagger-ui.html");
        log.debug("InternalAuthFilter - Is path {} Swagger UI HTML? {}", path, isSwaggerUI);

        return isSwaggerUI;
    }
}
