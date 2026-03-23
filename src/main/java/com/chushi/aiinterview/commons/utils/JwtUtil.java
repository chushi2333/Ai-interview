package com.chushi.aiinterview.commons.utils;

import com.chushi.aiinterview.entities.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("<SECRET>")
    private String secret;

    @Value("604800")
    @Getter
    private Long expiration;

    private SecretKey getSigningKey() { return Keys.hmacShaKeyFor(secret.getBytes()); }

    public String generateToken(User user) {
        var now = new Date();
        var expirationDate = new Date(now.getTime() + expiration * 1000);

        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("userId", user.getId())
                .issuedAt(now)
                .expiration(expirationDate)
                .signWith(getSigningKey())
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Long getUserIdFromToken(String token) {
        var claims = parseToken(token);
        return claims.get("userId", Long.class);
    }

    public boolean validateToken(String token) {
        try {
            parseToken(token);
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
        return true;
    }

    public boolean isTokenExpired(String token) {
        try {
            var claims = parseToken(token);
            return claims.getExpiration().before(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return true;
        }
    }
}
