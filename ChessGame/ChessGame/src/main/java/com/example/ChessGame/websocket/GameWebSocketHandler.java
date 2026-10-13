package com.example.ChessGame.websocket;

import com.example.ChessGame.service.GameService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class GameWebSocketHandler extends TextWebSocketHandler {

    private final AuthenticationManager authenticationManager;
    private final GameService gameService;
    private final ConcurrentMap<String, Set<WebSocketSession>> gameSessions =
            new ConcurrentHashMap<>();

    public GameWebSocketHandler(
            AuthenticationManager authenticationManager,
            GameService gameService) {
        this.authenticationManager = authenticationManager;
        this.gameService = gameService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session)
            throws IOException {
        session.sendMessage(new TextMessage("{\"type\":\"AUTH_REQUIRED\"}"));
    }

    @Override
    protected void handleTextMessage(
            WebSocketSession session, TextMessage message) throws IOException {
        if (session.getAttributes().containsKey("gameId")) {
            return;
        }

        try {
            String[] fields = message.getPayload().split("\\|", 4);
            if (fields.length != 4 || !"AUTH".equals(fields[0])) {
                closeForAuthentication(session);
                return;
            }

            String gameId = decode(fields[1]);
            String username = decode(fields[2]);
            String password = decode(fields[3]);
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password));

            // Only players in this game may subscribe to its updates.
            gameService.getGame(gameId, authentication.getName());
            session.getAttributes().put("gameId", gameId);
            session.getAttributes().put("username", authentication.getName());
            gameSessions.computeIfAbsent(
                    gameId, id -> ConcurrentHashMap.newKeySet()).add(session);
            session.sendMessage(new TextMessage("{\"type\":\"AUTHENTICATED\"}"));
        } catch (AuthenticationException | IllegalArgumentException exception) {
            closeForAuthentication(session);
        }
    }

    private String decode(String value) {
        return new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
    }

    public void broadcastGameUpdate(String gameId) {
        Set<WebSocketSession> sessions = gameSessions.get(gameId);
        if (sessions == null) {
            return;
        }

        TextMessage update = new TextMessage("{\"type\":\"GAME_UPDATED\"}");
        for (WebSocketSession session : sessions) {
            try {
                synchronized (session) {
                    if (session.isOpen()) {
                        session.sendMessage(update);
                    }
                }
            } catch (IOException exception) {
                removeSession(session);
            }
        }
    }

    @Override
    public void afterConnectionClosed(
            WebSocketSession session, CloseStatus status) {
        removeSession(session);
    }

    private void removeSession(WebSocketSession session) {
        Object gameId = session.getAttributes().get("gameId");
        if (gameId == null) {
            return;
        }

        Set<WebSocketSession> sessions = gameSessions.get(gameId.toString());
        if (sessions != null) {
            sessions.remove(session);
            if (sessions.isEmpty()) {
                gameSessions.remove(gameId.toString(), sessions);
            }
        }
    }

    private void closeForAuthentication(WebSocketSession session)
            throws IOException {
        session.close(CloseStatus.POLICY_VIOLATION);
    }
}
