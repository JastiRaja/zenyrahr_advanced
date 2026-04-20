package com.zenyrahr.hrms.utils;

import com.zenyrahr.hrms.model.Employee;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.MacAlgorithm; // Import MacAlgorithm
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {

    private final long EXPIRATION_TIME = 1000 * 60 * 60; // 1 hour
    private final MacAlgorithm ALGORITHM = Jwts.SIG.HS256; // Use MacAlgorithm
    private SecretKey SECRET_KEY; // SecretKey for signing

    @Value("${app.jwt.secret:ZGVmYXVsdC1qd3Qtc2VjcmV0LWNoYW5nZS1tZS1mb3ItemVueXJhSFI=}")
    private String jwtSecret;

    @PostConstruct
    public void init() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        this.SECRET_KEY = Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(String username) {
        return generateToken(username, null, null);
    }

    public String generateToken(Employee employee) {
        Long orgId = employee.getOrganization() != null ? employee.getOrganization().getId() : null;
        return generateToken(employee.getUsername(), employee.getRole(), orgId);
    }

    public String generateToken(String username, String role, Long organizationId) {
        Map<String, Object> claims = new HashMap<>();
        if (role != null) {
            claims.put("role", role);
        }
        if (organizationId != null) {
            claims.put("orgId", organizationId);
        }
        return Jwts.builder()
                .claims(claims)
                .subject(username) // Set subject
                .issuedAt(new Date()) // Set issued at time
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME)) // Set expiration
                .signWith(SECRET_KEY, ALGORITHM) // Sign with key and algorithm
                .compact(); // Build and serialize to a compact string
    }

    public String extractUsername(String token) {
        return Jwts.parser()
                .verifyWith(SECRET_KEY) // Verify with the same key
                .build()
                .parseSignedClaims(token) // Parse the token
                .getPayload() // Get the payload (claims)
                .getSubject(); // Extract the subject (username)
    }

    public String generateRefreshToken(String username) {
        return generateRefreshToken(username, null, null);
    }

    public String generateRefreshToken(Employee employee) {
        Long orgId = employee.getOrganization() != null ? employee.getOrganization().getId() : null;
        return generateRefreshToken(employee.getUsername(), employee.getRole(), orgId);
    }

    public String generateRefreshToken(String username, String role, Long organizationId) {
        Map<String, Object> claims = new HashMap<>();
        if (role != null) {
            claims.put("role", role);
        }
        if (organizationId != null) {
            claims.put("orgId", organizationId);
        }
        return Jwts.builder()
                .claims(claims)
                .subject(username) // Set subject
                .issuedAt(new Date()) // Set issued at time
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME * 24 * 7)) // 7 days
                .signWith(SECRET_KEY, ALGORITHM) // Sign with key and algorithm
                .compact(); // Build and serialize to a compact string
    }

    public Long extractOrganizationId(String token) {
        Object value = extractAllClaims(token).get("orgId");
        if (value instanceof Number number) {
            return number.longValue();
        }
        return null;
    }

    public boolean isTokenValid(String token, String username) {
        String extractedUsername = extractUsername(token); // Extract username from token
        return username.equals(extractedUsername) && !isTokenExpired(token); // Validate username and expiration
    }

    private boolean isTokenExpired(String token) {
        return extractAllClaims(token)
                .getExpiration() // Get expiration date
                .before(new Date()); // Check if expired
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(SECRET_KEY) // Verify with the same key
                .build()
                .parseSignedClaims(token) // Parse the token
                .getPayload(); // Get the payload (claims)
    }
}