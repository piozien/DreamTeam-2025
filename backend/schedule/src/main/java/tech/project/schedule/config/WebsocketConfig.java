package tech.project.schedule.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.messaging.converter.DefaultContentTypeResolver;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.security.messaging.context.AuthenticationPrincipalArgumentResolver;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.List;

/**
 * WebSocket configuration class for real-time communication in the scheduling application.
 * 
 * This class enables bidirectional communication between clients and server using the STOMP protocol
 * over WebSocket connections. It configures message brokers, endpoints, authentication integration,
 * and message format conversion to support real-time notifications and updates throughout the system.
 * 
 * The configuration uses a high precedence order to ensure proper initialization relative to other
 * components in the application.
 */
@Configuration
@EnableWebSocketMessageBroker
@Order(Ordered.HIGHEST_PRECEDENCE + 99)
@RequiredArgsConstructor
public class WebsocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * Configures the message broker settings.
     * Sets up:
     * - A simple in-memory broker for user-specific messages ("/user")
     * - Application destination prefixes for client-to-server messages ("/app")
     * - User destination prefix for user-targeted messages ("/user")
     * 
     * @param registry The MessageBrokerRegistry to configure
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/user");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    /**
     * Registers STOMP endpoints where clients can connect.
     * Configures the "/ws" endpoint with SockJS fallback support for browsers
     * that don't natively support WebSocket. Also configures CORS to allow
     * connections from the Angular frontend.
     * 
     * @param registry The StompEndpointRegistry to configure
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOrigins("http://localhost:4200")
                .withSockJS();
    }

    /**
     * Adds custom method argument resolvers for WebSocket controllers.
     * Specifically adds support for resolving the authenticated user principal
     * in message-handling methods.
     * 
     * @param argumentResolvers The list of resolvers to extend
     */
    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
        argumentResolvers.add(new AuthenticationPrincipalArgumentResolver());
    }

    /**
     * Configures message converters for WebSocket payloads.
     * Sets up a Jackson converter with JSON as the default content type,
     * enabling automatic conversion between JSON payloads and Java objects.
     * 
     * @param messageConverters The list of converters to configure
     * @return false to indicate standard converters should also be added
     */
    @Override
    public boolean configureMessageConverters(List<MessageConverter> messageConverters) {
        DefaultContentTypeResolver resolver = new DefaultContentTypeResolver();
        resolver.setDefaultMimeType(MediaType.APPLICATION_JSON);

        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setObjectMapper(new ObjectMapper());
        converter.setContentTypeResolver(resolver);
        messageConverters.add(converter);



        return false;
    }
}


