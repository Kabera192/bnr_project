package rw.bnr.user_service.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.Key;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class JwtService
{
    private final String secret;

    public JwtService(@Value("${jwt.secret}") String mySecret)
    {
//        log.info("JwtService constructor creating secret key generator.");
//        try
//        {
//            KeyGenerator keyGenerator = KeyGenerator.getInstance("HmacSHA256");
//            SecretKey secretKey = keyGenerator.generateKey();
//            secret = Base64.getEncoder().encodeToString(secretKey.getEncoded());
//        }
//        catch(Exception e)
//        {
//            log.error(e.getMessage());
//        }
        secret = mySecret;
    }

    public String generateToken(String email)
    {
        log.info("generateToken called with email: {}", email);
        Map<String, Object> claims = new HashMap<>();

        return Jwts.builder()
                .claims()
                .add(claims)
                .subject(email)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + 360000000))
                .and()
                .signWith(getKey())
                .compact();

    }

    public Key getKey()
    {
        log.info("getKey called");
        byte[] keyBytes = secret.getBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
