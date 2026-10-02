package com.gestao.temporeal.infraestrutura;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.Arrays;

/**
 * Tempo real com STOMP sobre WebSocket (ver docs/adr/0012).
 *
 * <ul>
 *   <li>Handshake em {@code /ws}, das mesmas origens liberadas no CORS</li>
 *   <li>Broker simples em memória para {@code /topic} (negociação) e {@code /queue} (avisos pessoais)</li>
 *   <li>Mensagens do cliente em {@code /app} (ex.: "digitando")</li>
 *   <li>Batimentos a cada 10 s para detectar conexões mortas</li>
 * </ul>
 *
 * A autenticação acontece no frame CONNECT, com o mesmo JWT da API ({@link AutenticacaoStomp}).
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private static final long BATIMENTO_MS = 10_000;

    private final AutenticacaoStomp autenticacao;

    @Value("${app.cors.allowed-origins:http://localhost:4200}")
    private String origensPermitidas;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint(Destinos.ENDPOINT)
                .setAllowedOrigins(Arrays.stream(origensPermitidas.split(","))
                        .map(String::trim)
                        .filter(origem -> !origem.isEmpty())
                        .toArray(String[]::new));
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        ThreadPoolTaskScheduler batimentos = new ThreadPoolTaskScheduler();
        batimentos.setPoolSize(1);
        batimentos.setThreadNamePrefix("ws-batimento-");
        batimentos.initialize();

        registry.enableSimpleBroker("/topic", "/queue")
                .setHeartbeatValue(new long[] {BATIMENTO_MS, BATIMENTO_MS})
                .setTaskScheduler(batimentos);
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(autenticacao);
    }
}
