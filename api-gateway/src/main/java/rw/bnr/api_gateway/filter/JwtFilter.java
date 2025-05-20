package rw.bnr.api_gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import rw.bnr.api_gateway.config.JwtProperties;
import rw.bnr.api_gateway.dto.ErrorResponse;
import rw.bnr.api_gateway.wrapper.HeaderMapRequestWrapper;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
@Order(1)
public class JwtFilter extends OncePerRequestFilter
{
    private final JwtProperties jwtProperties;
    private final List<String> publicPaths = Arrays.asList(
            "/user/register",
            "/user/login",
            "/swagger-ui"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException
    {
        log.info("JWT Filter - doFilterInternal function called.");
        String path = request.getRequestURI();

        // Skip authentication for public endpoints
        if (isPublicPath(path))
        {
            log.info("Public path: {} is part of public paths", path);
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");
        String token = null;

        if (authHeader != null && authHeader.startsWith("Bearer "))
        {
            token = authHeader.substring(7);
        }
        else
        {
            log.info("JWT Filter - no token found.");
            onError(response, "Invalid token.");
            return;
        }

        Claims claims = validateToken(token);
        if (claims != null)
        {
            log.info("JWT Filter - token valid.");

            Authentication auth = new UsernamePasswordAuthenticationToken(
                    claims.getSubject(),
                    null,
                    null
            );
            SecurityContextHolder.getContext().setAuthentication(auth);
            log.debug("Set Authentication in SecurityContext for user: {}", claims.getSubject());

            HeaderMapRequestWrapper requestWrapper = new HeaderMapRequestWrapper(request);

            // Add authentication headers for downstream services
            requestWrapper.addHeader("X-Internal-Auth", "true");

            filterChain.doFilter(requestWrapper, response);
            return;
        }

        log.info("JWT Filter - token invalid.");
        onError(response, "Invalid token.");
    }

    private void onError(HttpServletResponse response, String message) throws IOException
    {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");

        ErrorResponse errorResponse = new ErrorResponse(message);

        response.getWriter().write(errorResponse.toString());
    }

    private Claims validateToken(String token)
    {
        try
        {
            Claims claims = Jwts.parser()
                    .setSigningKey(Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes()))
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            if (claims.getExpiration().after(new Date(System.currentTimeMillis())))
            {
                log.debug("Token claims: {}, Expiration: {}", claims, claims.getExpiration());
                return claims;
            }
            else
            {
                log.debug("Token expired: {}", claims.getExpiration());
                return null;
            }
        }
        catch (Exception e)
        {
            log.error("Token validation failed: {}", e.getMessage());
            return null;
        }
    }

    private boolean isPublicPath(String path)
    {
        return publicPaths.stream().anyMatch(path::endsWith);
    }
}
