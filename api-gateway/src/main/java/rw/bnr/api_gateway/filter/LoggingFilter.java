package rw.bnr.api_gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.ContentCachingResponseWrapper;
import reactor.core.publisher.Mono;
import rw.bnr.api_gateway.dto.RequestLogDto;

import java.io.IOException;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
@Order(2) // Execute after JWT filter (which should be Order(1))
public class LoggingFilter extends OncePerRequestFilter
{
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private static final String KAFKA_TOPIC = "request-logs"; // Configure this in properties

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException
    {
        long startTime = System.currentTimeMillis();

        // Wrap response to capture status code
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);

        try
        {
            // Continue with the filter chain
            filterChain.doFilter(request, responseWrapper);
        }
        finally
        {
            // Log the request after it's processed
            logRequest(request, responseWrapper, startTime);

            // Don't forget to copy the cached response content to the actual response
            responseWrapper.copyBodyToResponse();
        }
    }

    private void logRequest(HttpServletRequest request, ContentCachingResponseWrapper response, long startTime)
    {
        try
        {
            // Extract user ID from SecurityContext if available
            String userId = extractUserIdFromSecurityContext();

            // Get client IP address
            String clientIp = getClientIpAddress(request);

            // Extract service name from the request path
            String service = extractServiceFromPath(request.getRequestURI());

            // Build the log DTO
            RequestLogDto logDto = RequestLogDto.builder()
                    .service(service)
                    .ip(clientIp)
                    .method(request.getMethod())
                    .path(request.getRequestURI())
                    .status(response.getStatus())
                    .userId(userId)
                    .timestamp(LocalDateTime.now())
                    .build();

            // Send to Kafka topic
            sendToKafka(logDto);

            // Log locally for debugging
            long duration = System.currentTimeMillis() - startTime;
            log.info("Request logged: {} {} {} - Status: {} - Duration: {}ms - User: {}",
                    request.getMethod(),
                    request.getRequestURI(),
                    clientIp,
                    response.getStatus(),
                    duration,
                    userId != null ? userId : "anonymous");

        }
        catch (Exception e)
        {
            log.error("Error logging request: {}", e.getMessage(), e);
        }
    }

    private String extractUserIdFromSecurityContext()
    {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal()))
        {
            return authentication.getName(); // This will be the subject from JWT claims
        }
        return null;
    }

    private String getClientIpAddress(HttpServletRequest request)
    {
        // Check for X-Forwarded-For header (common in load balancers/proxies)
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor))
        {
            // X-Forwarded-For can contain multiple IPs, get the first one
            return xForwardedFor.split(",")[0].trim();
        }

        // Check for X-Real-IP header
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp))
        {
            return xRealIp;
        }

        // Fall back to remote address
        return request.getRemoteAddr();
    }

    private String extractServiceFromPath(String path)
    {
        // Extract service name from the path
        // Assuming paths are like /user/login, /product/search, etc.
        if (path != null && path.startsWith("/"))
        {
            String[] pathParts = path.split("/");
            if (pathParts.length > 1)
            {
                return pathParts[1]; // Return the first part after the leading slash
            }
        }
        return "unknown";
    }

    private void sendToKafka(RequestLogDto logDto)
    {
        try
        {
            String logJson = objectMapper.writeValueAsString(logDto);
            kafkaTemplate.send(KAFKA_TOPIC, logJson);
            log.debug("Request log sent to Kafka topic: {}", KAFKA_TOPIC);
        }
        catch (Exception e)
        {
            log.error("Failed to send request log to Kafka: {}", e.getMessage(), e);
        }
    }
}