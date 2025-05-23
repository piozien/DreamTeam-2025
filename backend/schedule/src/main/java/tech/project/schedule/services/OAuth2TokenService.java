package tech.project.schedule.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import tech.project.schedule.exception.ApiException;
import tech.project.schedule.model.user.User;
import tech.project.schedule.repositories.UserRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service that manages OAuth2 tokens for Google Calendar integration.
 * Handles refreshing tokens when they expire and maintaining client authentication
 * for API requests.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OAuth2TokenService {
    private final UserRepository userRepository;

    
    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String clientId;
    
    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String clientSecret;
    
    // Cache for access tokens to avoid unnecessary refreshes
    private final Map<UUID, CachedTokenInfo> tokenCache = new ConcurrentHashMap<>();
    
    /**
     * Gets a valid OAuth2 access token for the specified user.
     * If the cached token has expired, it refreshes it using the stored refresh token.
     * 
     * @param userId ID of the user to get token for
     * @return A valid access token or null if not available
     */
    public String getAccessToken(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found", org.springframework.http.HttpStatus.NOT_FOUND));
        
        if (user.getGoogleRefreshToken() == null) {
            log.warn("No refresh token available for user {}", userId);
            return null;
        }
        
        // Check if we have a valid cached token
        CachedTokenInfo cachedToken = tokenCache.get(userId);
        if (cachedToken != null && !cachedToken.isExpired()) {
            return cachedToken.accessToken;
        }
        
        // Need to refresh the token
        try {
            String newAccessToken = refreshAccessToken(user.getGoogleRefreshToken());
            
            // Cache the new token (typically valid for 1 hour)
            tokenCache.put(userId, new CachedTokenInfo(
                    newAccessToken,
                    Instant.now().plus(Duration.ofMinutes(55)) // Slightly less than the actual expiry
            ));
            
            return newAccessToken;
        } catch (Exception e) {
            log.error("Failed to refresh access token for user {}: {}", userId, e.getMessage());
            return null;
        }
    }

    /**
     * Uses a refresh token to obtain a new access token from Google OAuth2 server.
     * 
     * @param refreshToken The refresh token to use
     * @return New access token
     */
    private String refreshAccessToken(String refreshToken) {
        RestTemplate restTemplate = new RestTemplate();
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED);
        
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("refresh_token", refreshToken);
        body.add("grant_type", "refresh_token");
        
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
        
        ResponseEntity<Map> response = restTemplate.exchange(
                "https://oauth2.googleapis.com/token",
                HttpMethod.POST,
                request,
                Map.class
        );
        
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = response.getBody();
        
        if (responseBody != null && responseBody.containsKey("access_token")) {
            return (String) responseBody.get("access_token");
        } else {
            throw new ApiException("Failed to refresh access token", org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Inner class for caching access tokens with their expiration time.
     */
    private static class CachedTokenInfo {
        private final String accessToken;
        private final Instant expiresAt;

        public CachedTokenInfo(String accessToken, Instant expiresAt) {
            this.accessToken = accessToken;
            this.expiresAt = expiresAt;
        }

        public boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }
}
