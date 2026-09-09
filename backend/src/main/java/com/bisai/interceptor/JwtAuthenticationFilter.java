package com.bisai.interceptor;

import com.bisai.entity.User;
import com.bisai.mapper.UserMapper;
import com.bisai.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, UserMapper userMapper) {
        this.jwtUtil = jwtUtil;
        this.userMapper = userMapper;
    }

    @Override
    protected void doFilterInternal(@org.springframework.lang.NonNull HttpServletRequest request,
                                    @org.springframework.lang.NonNull HttpServletResponse response,
                                    @org.springframework.lang.NonNull FilterChain filterChain) throws ServletException, IOException {
        String token = resolveToken(request);

        if (StringUtils.hasText(token)) {
            if (!jwtUtil.isTokenExpired(token) && isPasswordVersionValid(token)) {
                Long userId = jwtUtil.getUserId(token);
                String username = jwtUtil.getUsername(token);
                String role = jwtUtil.getRole(token);

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userId, null,
                                List.of(new SimpleGrantedAuthority("ROLE_" + role))
                        );

                SecurityContextHolder.getContext().setAuthentication(authentication);

                request.setAttribute("userId", userId);
                request.setAttribute("username", username);
                request.setAttribute("role", role);
            } else {
                String path = request.getRequestURI();
                if (!path.startsWith("/api/auth/")) {
                    log.debug("JWT token无效或已过期: path={}, ip={}", path, request.getRemoteAddr());
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    /**
     * 密码版本校验：token 内 pwd claim 必须不落后于用户当前 lastPasswordChangeAt，
     * 改密/管理员重置密码后旧 token 立即失效；顺带拒绝已禁用/已删除的账号。
     */
    private boolean isPasswordVersionValid(String token) {
        try {
            Long userId = jwtUtil.getUserId(token);
            User user = userMapper.selectById(userId);
            if (user == null || "DISABLED".equals(user.getStatus())) {
                return false;
            }
            return jwtUtil.getTokenPwdVersion(token) >= JwtUtil.pwdVersion(user.getLastPasswordChangeAt());
        } catch (Exception e) {
            log.debug("JWT 密码版本校验失败: {}", e.getMessage());
            return false;
        }
    }
}
