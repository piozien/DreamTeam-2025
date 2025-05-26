package tech.project.schedule.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.security.Key;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;

/**
 * Utility class for generating, validating, and extracting information from JSON Web Tokens (JWT).
 * Handles token creation, claim extraction, and signature verification for authentication and authorization.
 */
@Component
public class JwtUtil {
    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expirationMs;

    /**
     * Generates the signing key used for JWT signature verification.
     *
     * @return Key object based on the configured secret
     */
    private Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Generates a JWT token for the given subject and claims.
     *
     * @param subject the subject (e.g., username or email) for whom the token is generated
     * @param claims additional claims to include in the token
     * @return the generated JWT token as a String
     */
    public String generateToken(String subject, Map<String, Object> claims) {
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(getSignInKey())
                .compact();
    }

    /**
     * Extracts the username (subject) from the given JWT token.
     *
     * @param token the JWT token
     * @return the subject (username/email) extracted from the token
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extracts a specific claim from the JWT token using a resolver function.
     *
     * @param token the JWT token
     * @param claimsResolver function to extract a specific claim from the claims object
     * @param <T> the type of the claim to extract
     * @return the extracted claim
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Extracts all claims from the JWT token after signature verification.
     *
     * @param token the JWT token
     * @return Claims object containing all claims from the token
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith((SecretKey) getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Validates the JWT token by checking its expiration date.
     *
     * @param token the JWT token
     * @return true if the token is valid and not expired; false otherwise
     */
    public boolean isTokenValid(String token) {
        try {
            final Date expiration = extractClaim(token, Claims::getExpiration);
            return expiration.after(new Date());
        } catch (Exception e) {
            return false;
        }
    }
}
