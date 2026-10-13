package com.example.ChessGame;

import com.example.ChessGame.model.Entity.Game;
import com.example.ChessGame.model.Entity.Player;
import com.example.ChessGame.model.Entity.Position;
import com.example.ChessGame.model.Enum.Color;
import com.example.ChessGame.model.Enum.GameStatus;
import com.example.ChessGame.repo.GameRepo;
import com.example.ChessGame.service.GameService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GameServiceRulesTest {

    private GameService gameService;

    @BeforeEach
    void setUp() {
        gameService = new GameService(new GameRepo());
    }

    @Test
    void whiteMovesFirstAndBlackCannotMoveOutOfTurn() {
        Game game = createGame();

        assertEquals(Color.WHITE, game.getCurrentTurn());

        assertThrows(IllegalArgumentException.class, () ->
                gameService.makeMove(
                        game.getId(),
                        game.getBlackPlayer().getName(),
                        "e7",
                        "e5"
                )
        );

        assertEquals(Color.WHITE, game.getCurrentTurn());

        gameService.makeMove(
                game.getId(),
                game.getWhitePlayer().getName(),
                "e2",
                "e4"
        );

        assertEquals(Color.BLACK, game.getCurrentTurn());
    }

    @Test
    void illegalMoveDoesNotChangeBoardOrTurn() {
        Game game = createGame();
        String gameId = game.getId();

        assertThrows(IllegalArgumentException.class, () ->
                gameService.makeMove(
                        gameId,
                        game.getWhitePlayer().getName(),
                        "e2",
                        "e5"
                )
        );

        Game updatedGame = gameService.getGame(gameId);

        assertEquals(Color.WHITE, updatedGame.getCurrentTurn());
        assertTrue(updatedGame.getMoves().isEmpty());
        assertNotNull(updatedGame.getBoard()
                .getSquare(new Position(1, 4)).getPiece()); // pawn remains on e2
        assertNull(updatedGame.getBoard()
                .getSquare(new Position(4, 4)).getPiece()); // e5 remains empty
    }

    @Test
    void foolsMateEndsTheGameInCheckmate() {
        Game game = createGame();
        String white = game.getWhitePlayer().getName();
        String black = game.getBlackPlayer().getName();

        gameService.makeMove(game.getId(), white, "f2", "f3");
        gameService.makeMove(game.getId(), black, "e7", "e5");
        gameService.makeMove(game.getId(), white, "g2", "g4");
        gameService.makeMove(game.getId(), black, "d8", "h4");

        assertEquals(GameStatus.CHECKMATE, game.getStatus());
    }

    @Test
    void undoLastMoveRestoresBoardTurnAndMoveHistory() {
        Game game = createGame();
        String gameId = game.getId();
        String whitePlayer = game.getWhitePlayer().getName();

        gameService.makeMove(gameId, whitePlayer, "e2", "e4");

        Game updatedGame = gameService.getGame(gameId);
        assertEquals(Color.BLACK, updatedGame.getCurrentTurn());
        assertEquals(1, updatedGame.getMoves().size());

        gameService.undoLastMove(gameId, whitePlayer);

        Game undoneGame = gameService.getGame(gameId);
        assertEquals(Color.WHITE, undoneGame.getCurrentTurn());
        assertTrue(undoneGame.getMoves().isEmpty());

        assertNotNull(undoneGame.getBoard()
                .getSquare(new Position(1, 4)).getPiece()); // pawn back on e2
        assertNull(undoneGame.getBoard()
                .getSquare(new Position(3, 4)).getPiece()); // e4 is empty
    }

    @Test
    void undoCaptureRestoresBothPieces() {
        Game game = createGame();
        String gameId = game.getId();
        String whitePlayer = game.getWhitePlayer().getName();
        String blackPlayer = game.getBlackPlayer().getName();

        gameService.makeMove(gameId, whitePlayer, "e2", "e4");
        gameService.makeMove(gameId, blackPlayer, "d7", "d5");
        gameService.makeMove(gameId, whitePlayer, "e4", "d5");

        assertEquals(1, game.getWhitePlayer().getCapturedPiecePoints());

        gameService.undoLastMove(gameId, whitePlayer);

        Game undoneGame = gameService.getGame(gameId);

        assertEquals(2, undoneGame.getMoves().size());
        assertNotNull(undoneGame.getBoard()
                .getSquare(new Position(3, 4)).getPiece());
        assertNotNull(undoneGame.getBoard()
                .getSquare(new Position(4, 3)).getPiece());
        assertEquals(Color.WHITE, undoneGame.getCurrentTurn());
        assertEquals(0, undoneGame.getWhitePlayer().getCapturedPiecePoints());
    }

    @Test
    void undoEnPassantRestoresBothPawns() {
        Game game = createGame();
        String gameId = game.getId();
        String white = game.getWhitePlayer().getName();
        String black = game.getBlackPlayer().getName();

        gameService.makeMove(gameId, white, "e2", "e4");
        gameService.makeMove(gameId, black, "a7", "a6");
        gameService.makeMove(gameId, white, "e4", "e5");
        gameService.makeMove(gameId, black, "d7", "d5");
        gameService.makeMove(gameId, white, "e5", "d6"); // en passant

        assertEquals(1, game.getWhitePlayer().getCapturedPiecePoints());

        gameService.undoLastMove(gameId, white);

        Game undoneGame = gameService.getGame(gameId);

        assertEquals(4, undoneGame.getMoves().size());
        assertEquals(Color.WHITE, undoneGame.getCurrentTurn());
        assertNotNull(undoneGame.getBoard()
                .getSquare(new Position(4, 4)).getPiece()); // white pawn back on e5
        assertNotNull(undoneGame.getBoard()
                .getSquare(new Position(4, 3)).getPiece()); // black pawn restored on d5
        assertNull(undoneGame.getBoard()
                .getSquare(new Position(5, 3)).getPiece()); // d6 is empty
        assertEquals(0, undoneGame.getWhitePlayer().getCapturedPiecePoints());

        // The en passant opportunity from Black's previous move is restored too.
        gameService.makeMove(gameId, white, "e5", "d6");
        assertEquals(5, gameService.getGame(gameId).getMoves().size());
    }

    @Test
    void undoCastlingRestoresKingRookAndCastlingRight() {
        Game game = createGame();
        String gameId = game.getId();
        String white = game.getWhitePlayer().getName();
        String black = game.getBlackPlayer().getName();

        gameService.makeMove(gameId, white, "e2", "e4");
        gameService.makeMove(gameId, black, "a7", "a6");
        gameService.makeMove(gameId, white, "g1", "f3");
        gameService.makeMove(gameId, black, "a6", "a5");
        gameService.makeMove(gameId, white, "f1", "e2");
        gameService.makeMove(gameId, black, "a5", "a4");
        gameService.makeMove(gameId, white, "e1", "g1");

        gameService.undoLastMove(gameId, white);

        Game undoneGame = gameService.getGame(gameId);
        assertEquals(6, undoneGame.getMoves().size());
        assertNotNull(undoneGame.getBoard()
                .getSquare(new Position(0, 4)).getPiece()); // king on e1
        assertNotNull(undoneGame.getBoard()
                .getSquare(new Position(0, 7)).getPiece()); // rook on h1
        assertNull(undoneGame.getBoard()
                .getSquare(new Position(0, 5)).getPiece()); // f1 empty
        assertNull(undoneGame.getBoard()
                .getSquare(new Position(0, 6)).getPiece()); // g1 empty

        // Retry castling to ensure the right was restored.
        gameService.makeMove(gameId, white, "e1", "g1");
    }

    private Game createGame() {
        gameService.joinMatchmaking(new Player("Alice", 1200));
        String secondTicket =
                gameService.joinMatchmaking(new Player("Bob", 1200));

        String gameId = gameService.getMatchedGameId(secondTicket)
                .orElseThrow();

        return gameService.getGame(gameId);
    }
}
