package com.bisai.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(Long userId, String username, String role) {
        return generateToken(userId, username, role, null);
    }

    /**
     * 签发携带密码版本（pwd claim = lastPasswordChangeAt 毫秒值）的 token；
     * 用户改密/管理员重置密码后，旧 token 的 pwd 版本落后即失效（无需 Redis 黑名单）。
     */
    public String generateToken(Long userId, String username, String role, java.time.LocalDateTime pwdChangedAt) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("role", role);
        claims.put("pwd", pwdVersion(pwdChangedAt));

        return Jwts.builder()
                .claims(claims)
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    /** 密码版本统一计算口径（签发与校验两侧共用），空视为 0 */
    public static long pwdVersion(java.time.LocalDateTime pwdChangedAt) {
        return pwdChangedAt == null ? 0L
                : pwdChangedAt.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    /** 读取 token 内的密码版本；无该 claim 的旧 token 返回 0 */
    public long getTokenPwdVersion(String token) {
        Claims claims = parseToken(token);
        Object v = claims.get("pwd");
        return v == null ? 0L : ((Number) v).longValue();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Long getUserId(String token) {
        Claims claims = parseToken(token);
        return claims.get("userId", Long.class);
    }

    public String getUsername(String token) {
        return parseToken(token).getSubject();
    }

    public String getRole(String token) {
        return parseToken(token).get("role", String.class);
    }

    public boolean isTokenExpired(String token) {
        try {
            return parseToken(token).getExpiration().before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        } catch (JwtException e) {
            log.warn("JWT校验失败: {}", e.getMessage());
            return true;
        }
    }
}
