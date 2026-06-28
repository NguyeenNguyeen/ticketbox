package com.ticketbox.backend.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.HexFormat;
import com.ticketbox.backend.repository.UserRepository;
import com.ticketbox.backend.entity.User;

@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpirationInMs;

    private final UserRepository userRepository;

    public JwtTokenProvider(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public String generateToken(Authentication authentication) {
        UserDetails userPrincipal = (UserDetails) authentication.getPrincipal();
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationInMs);

        String role = userPrincipal.getAuthorities().stream()
                .map(grantedAuthority -> grantedAuthority.getAuthority().replace("ROLE_", ""))
                .findFirst()
                .orElse("CUSTOMER");

        User user = userRepository.findByUsername(userPrincipal.getUsername()).orElse(null);
        String email = user != null && user.getEmail() != null ? user.getEmail() : userPrincipal.getUsername() + "@ticketbox.vn";
        String name = user != null && user.getFullName() != null && !user.getFullName().isEmpty() 
                        ? user.getFullName() 
                        : userPrincipal.getUsername();

        return Jwts.builder()
                .subject(userPrincipal.getUsername())
                .claim("role", role)
                .claim("email", email)
                .claim("name", name)
                .issuedAt(new Date())
                .expiration(expiryDate)
                .signWith(key())
                .compact();
    }

    private SecretKey key() {
        byte[] keyBytes;
        if (jwtSecret.matches("^[0-9A-Fa-f]+$") && jwtSecret.length() % 2 == 0) {
            keyBytes = HexFormat.of().parseHex(jwtSecret);
        } else {
            keyBytes = Decoders.BASE64.decode(jwtSecret);
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String getUsernameFromJWT(String token) {
        return Jwts.parser()
                .verifyWith(key())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public boolean validateToken(String authToken) {
        try {
            Jwts.parser().verifyWith(key()).build().parseSignedClaims(authToken);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            // Log error
        }
        return false;
    }
}
