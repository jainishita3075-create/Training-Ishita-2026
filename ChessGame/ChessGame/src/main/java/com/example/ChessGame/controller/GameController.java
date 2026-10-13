package com.example.ChessGame.controller;

import com.example.ChessGame.dto.GameStateResponse;
import com.example.ChessGame.dto.MoveRequest;
import com.example.ChessGame.model.Entity.Board;
import com.example.ChessGame.model.Entity.Game;
import com.example.ChessGame.model.Entity.Move;
import com.example.ChessGame.model.Entity.Piece;
import com.example.ChessGame.model.Entity.Position;
import com.example.ChessGame.model.Enum.Color;
import com.example.ChessGame.service.GameService;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.security.Principal;

@RestController
@RequestMapping("/api/games")
public class GameController {

    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    @PostMapping("/{gameId}/moves")
    public GameStateResponse makeMove(
            @PathVariable String gameId,
            @RequestBody MoveRequest request,
            Principal principal) {

        gameService.makeMove(
                gameId,
                principal.getName(),
                request.getFrom(),
                request.getTo(),
                request.getPromotion()
        );

        return getGame(gameId, principal);
    }

    @GetMapping("/{gameId}")
    public GameStateResponse getGame(
            @PathVariable String gameId,
            Principal principal) {
        Game game = gameService.getGame(gameId, principal.getName());

        return new GameStateResponse(
                game.getId(),
                game.getWhitePlayer().getName(),
                game.getBlackPlayer().getName(),
                game.getWhitePlayer().getEloRating(),
                game.getBlackPlayer().getEloRating(),
                game.getWhitePlayer().getCapturedPiecePoints(),
                game.getBlackPlayer().getCapturedPiecePoints(),
                game.getCurrentTurn().toString(),
                game.getStatus().toString(),
                createBoardView(game.getBoard()),
                createMoveHistory(game.getMoves())
        );
    }

    @PostMapping("/{gameId}/resign")
    public GameStateResponse resign(
            @PathVariable String gameId,
            Principal principal) {

        gameService.resignGame(gameId, principal.getName());
        return getGame(gameId, principal);
    }

    @PostMapping("/{gameId}/undo")
    public GameStateResponse undo(
            @PathVariable String gameId,
            Principal principal) {
        gameService.undoLastMove(gameId, principal.getName());
        return getGame(gameId, principal);
    }

    private List<List<String>> createBoardView(Board board) {
        List<List<String>> rows = new ArrayList<>();

        for (int row = 7; row >= 0; row--) {
            List<String> columns = new ArrayList<>();

            for (int column = 0; column < 8; column++) {
                Position position = new Position(row, column);
                Piece piece = board.getSquare(position).getPiece();

                columns.add(piece == null ? "" : getPieceSymbol(piece));
            }

            rows.add(columns);
        }

        return rows;
    }

    private String getPieceSymbol(Piece piece) {
        String symbol = switch (piece.getPieceType()) {
            case KING -> "K";
            case QUEEN -> "Q";
            case ROOK -> "R";
            case BISHOP -> "B";
            case KNIGHT -> "N";
            case PAWN -> "P";
        };

        return piece.getColor() == Color.WHITE
                ? symbol
                : symbol.toLowerCase();
    }

    private List<String> createMoveHistory(List<Move> moves) {
        List<String> history = new ArrayList<>();

        for (Move move : moves) {
            history.add(
                    toSquareName(move.getFrom())
                            + "-"
                            + toSquareName(move.getTo())
            );
        }

        return history;
    }

    private String toSquareName(Position position) {
        char file = (char) ('a' + position.getColumn());
        int rank = position.getRow() + 1;

        return "" + file + rank;
    }
}
