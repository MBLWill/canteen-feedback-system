package com.qmz.canteen.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    private SecretKey key() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(LoginUser user) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(user.getUserId()))
                .claim("username", user.getUsername())
                .claim("realName", user.getRealName())
                .claim("role", user.getRole())
                .claim("stallId", user.getStallId())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expiration))
                .signWith(key())
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(key())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public LoginUser parseLoginUser(String token) {
        try {
            Claims claims = parseToken(token);
            LoginUser user = new LoginUser();
            user.setUserId(Long.valueOf(claims.getSubject()));
            user.setUsername(claims.get("username", String.class));
            user.setRealName(claims.get("realName", String.class));
            user.setRole(claims.get("role", String.class));
            Object stallId = claims.get("stallId");
            user.setStallId(stallId == null ? null : Long.valueOf(stallId.toString()));
            return user;
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }
}
