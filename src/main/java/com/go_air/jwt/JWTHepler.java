package com.go_air.jwt;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.security.Key;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

@Component
public class JWTHepler {

    public static final long JWT_TOKEN_VALIDITY = 5 * 60 * 60; // 5 hours
    private static final String SECRET = "afafasfafafasfasfasfafacasdasfasxASFACASDFACASDFASFASFDAFASFASDAADSCSDFADCVSGCFVADXCcadwavfsfarvf";

    private final Key secretKey;

    public JWTHepler() {
        this.secretKey = Keys.hmacShaKeyFor(SECRET.getBytes());
    }

    // Retrieve username from token
    public String getUsernameFromToken(String token) {
        return getClaimFromToken(token, Claims::getSubject);
    }

    // Retrieve expiration date from token
    public Date getExpirationDateFromToken(String token) {
        return getClaimFromToken(token, Claims::getExpiration);
    }

    // Retrieve specific claim
    public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }

    // Retrieve all claims
    private Claims getAllClaimsFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // Check if token expired
    private Boolean isTokenExpired(String token) {
        final Date expiration = getExpirationDateFromToken(token);
        return expiration.before(new Date());
    }

    // Generate token and include role
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();

        // Take first authority, remove "ROLE_" if present
        String fullRole = userDetails.getAuthorities().iterator().next().getAuthority();
        String role = fullRole.startsWith("ROLE_") ? fullRole.substring(5) : fullRole;
        claims.put("role", role);

        return doGenerateToken(claims, userDetails.getUsername());
    }

    // Core token generation
    private String doGenerateToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + JWT_TOKEN_VALIDITY * 1000))
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    // Validate token
    public Boolean validateToken(String token, UserDetails userDetails) {
        final String username = getUsernameFromToken(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    // Extract role from token
    public String getRoleFromToken(String token) {
        final Claims claims = getAllClaimsFromToken(token);
        return (String) claims.get("role");
    }
}
