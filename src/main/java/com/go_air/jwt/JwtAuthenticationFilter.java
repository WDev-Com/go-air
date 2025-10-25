package com.go_air.jwt;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.HashMap;
import java.util.Map;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Autowired
    private JWTHepler jwtHelper;

    @Autowired
    @Lazy
    private UserDetailsService userDetailsService;

    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    // Extract token from Authorization header
    public String extractTokenFromRequest(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7); // Remove "Bearer " prefix
        }
        return null;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getServletPath();

        // Skip public endpoints
        if (path.startsWith("/auth/login") ||
            path.startsWith("/auth/signup") ||
            path.startsWith("/auth/refreshToken") ||
            path.startsWith("/auth/checkusername")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = extractTokenFromRequest(request);

        if (token != null) {
            // Check if token is blacklisted
            if (tokenBlacklistService.isTokenBlacklisted(token)) {
                response.setContentType("application/json");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                Map<String, String> resp = new HashMap<>();
                resp.put("status", "FAILED");
                resp.put("message", "Token is blacklisted. Please login again.");
                response.getWriter().write(new ObjectMapper().writeValueAsString(resp));
                return;
            }

            String username = jwtHelper.getUsernameFromToken(token);
            String role = jwtHelper.getRoleFromToken(token);

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                if (jwtHelper.validateToken(token, userDetails)) {
                    // Set authorities based on token role
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    List.of(new SimpleGrantedAuthority(role))
                            );
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    logger.info("Authenticated user: {}, Role: {}", username, role);

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
