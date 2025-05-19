package rw.bnr.api_gateway.config;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jwt")
@Data
@NoArgsConstructor
public class JwtProperties
{
    private String secret;

    public JwtProperties(@Value("${jwt.secret}") String jwtSecret)
    {
        secret = jwtSecret;
    }
}
