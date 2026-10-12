package com.example.ChessGame.controller;

import com.example.ChessGame.dto.JoinMatchRequest;
import com.example.ChessGame.dto.MatchMakingResponse;
import com.example.ChessGame.model.Entity.Player;
import com.example.ChessGame.service.GameService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/matchmaking")
public class MatchMakingController {
    private final GameService gameService;

    public MatchMakingController(GameService gameService) {
        this.gameService = gameService;
    }

    @PostMapping("/join")
    public ResponseEntity<MatchMakingResponse> join(
            @RequestBody JoinMatchRequest request) {

        if (request.getName() == null || request.getName().isBlank()) {
            return ResponseEntity.badRequest().body(
                    new MatchMakingResponse(
                            false,
                            "Player name is required.",
                            null,
                            null
                    )
            );
        }

        if (request.getEloRating() < 0) {
            return ResponseEntity.badRequest().body(
                    new MatchMakingResponse(
                            false,
                            "Elo rating cannot be negative.",
                            null,
                            null
                    )
            );
        }

        Player player = new Player(
                request.getName(),
                request.getEloRating()
        );

        String ticketId = gameService.joinMatchmaking(player);

        Optional<String> gameId =
                gameService.getMatchedGameId(ticketId);

        if (gameId.isPresent()) {
            return ResponseEntity.ok(new MatchMakingResponse(
                    true,
                    "Opponent found.",
                    ticketId,
                    gameId.get()
            ));
        }

        return ResponseEntity.accepted().body(new MatchMakingResponse(
                false,
                "Waiting for an opponent in your Elo group.",
                ticketId,
                null
        ));
    }

    @GetMapping("/{ticketId}")
    public ResponseEntity<MatchMakingResponse> checkMatch(
            @PathVariable String ticketId) {

        Optional<String> gameId =
                gameService.getMatchedGameId(ticketId);

        if (gameId.isPresent()) {
            return ResponseEntity.ok(new MatchMakingResponse(
                    true,
                    "Opponent found.",
                    ticketId,
                    gameId.get()
            ));
        }

        return ResponseEntity.accepted().body(new MatchMakingResponse(
                false,
                "Still waiting for an opponent.",
                ticketId,
                null
        ));
    }
}
