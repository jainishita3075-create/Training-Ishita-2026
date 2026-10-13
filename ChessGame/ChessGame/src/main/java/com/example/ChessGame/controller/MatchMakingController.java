package com.example.ChessGame.controller;

import com.example.ChessGame.dto.MatchMakingResponse;
import com.example.ChessGame.model.Entity.Player;
import com.example.ChessGame.service.AccountService;
import com.example.ChessGame.service.GameService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Optional;

@RestController
@RequestMapping("/api/matchmaking")
public class MatchMakingController {
    private final GameService gameService;
    private final AccountService accountService;

    public MatchMakingController(
            GameService gameService,
            AccountService accountService) {
        this.gameService = gameService;
        this.accountService = accountService;
    }

    @PostMapping("/join")
    public ResponseEntity<MatchMakingResponse> join(Principal principal) {
        String playerName = principal.getName();
        Player player = accountService.createPlayer(playerName);

        String ticketId = gameService.joinMatchmaking(player);

        Optional<String> gameId =
                gameService.getMatchedGameId(ticketId, playerName);

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
            @PathVariable String ticketId,
            Principal principal) {

        Optional<String> gameId =
                gameService.getMatchedGameId(ticketId, principal.getName());

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
