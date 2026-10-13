package com.example.ChessGame.service;

import com.example.ChessGame.model.Entity.Game;
import com.example.ChessGame.model.Entity.Move;
import com.example.ChessGame.model.Entity.Piece;
import com.example.ChessGame.model.Entity.Player;
import com.example.ChessGame.model.Entity.Position;
import com.example.ChessGame.model.Enum.Color;
import com.example.ChessGame.model.Enum.GameStatus;
import com.example.ChessGame.model.Enum.MoveType;
import com.example.ChessGame.model.Enum.PieceType;
import com.example.ChessGame.repo.GameRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class GameService {

    private final GameRepo gameRepository;
    private final AccountService accountService;
    private final PieceFactory pieceFactory = new PieceFactory();

    private final Map<String, Player> waitingPlayers = new HashMap<>();
    private final Map<String, String> matchedGames = new HashMap<>();
    private final Map<String, String> ticketOwners = new HashMap<>();

    @Autowired
    public GameService(GameRepo gameRepository, AccountService accountService) {
        this.gameRepository = gameRepository;
        this.accountService = accountService;
    }

    // Keeps simple service construction available for unit tests.
    public GameService(GameRepo gameRepository) {
        this.gameRepository = gameRepository;
        this.accountService = null;
    }

    public String joinMatchmaking(Player player) {
        if (player == null) {
            throw new IllegalArgumentException("Player is required.");
        }

        String ticketId = UUID.randomUUID().toString();
        ticketOwners.put(ticketId, player.getName());

        Optional<Map.Entry<String, Player>> opponent =
                waitingPlayers.entrySet()
                        .stream()
                        .filter(entry ->
                                getRatingGroup(entry.getValue().getEloRating())
                                        == getRatingGroup(player.getEloRating()))
                        .min(Comparator.comparingInt(entry ->
                                Math.abs(entry.getValue().getEloRating()
                                        - player.getEloRating())));

        if (opponent.isEmpty()) {
            waitingPlayers.put(ticketId, player);
            return ticketId;
        }

        String opponentTicketId = opponent.get().getKey();
        Player opponentPlayer = opponent.get().getValue();

        waitingPlayers.remove(opponentTicketId);

        Game game = new Game(player, opponentPlayer, pieceFactory);
        Game savedGame = gameRepository.save(game);

        // Both players can use their own ticket to find the same game.
        matchedGames.put(ticketId, savedGame.getId());
        matchedGames.put(opponentTicketId, savedGame.getId());

        return ticketId;
    }

    public Optional<String> getMatchedGameId(String ticketId) {
        if (ticketId == null || ticketId.isBlank()) {
            throw new IllegalArgumentException("Matchmaking ticket is required.");
        }

        return Optional.ofNullable(matchedGames.get(ticketId));
    }

    public Optional<String> getMatchedGameId(
            String ticketId,
            String playerName) {
        String owner = ticketOwners.get(ticketId);
        if (owner == null || !owner.equalsIgnoreCase(playerName)) {
            throw new IllegalArgumentException(
                    "This matchmaking ticket does not belong to you.");
        }
        return getMatchedGameId(ticketId);
    }

    public Game getGame(String gameId) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Game not found: " + gameId));
        Color timedOutPlayer = game.updateClock();
        if (timedOutPlayer != null) {
            updateRatings(
                    game.getWhitePlayer(),
                    game.getBlackPlayer(),
                    timedOutPlayer == Color.WHITE ? 0.0 : 1.0
            );
            gameRepository.save(game);
        }
        return game;
    }

    public Game getGame(String gameId, String playerName) {
        Game game = getGame(gameId);
        findPlayer(game, playerName);
        return game;
    }

    private int getRatingGroup(int rating) {
        if (rating < 800) return 0;
        if (rating < 1200) return 1;
        if (rating < 1600) return 2;
        if (rating < 2000) return 3;
        if (rating < 2200) return 4;
        if (rating < 2500) return 5;
        return 6;
    }

    public Game makeMove(
            String gameId,
            String playerName,
            String fromText,
            String toText) {
        return makeMove(gameId, playerName, fromText, toText, null);
    }

    public Game makeMove(
            String gameId,
            String playerName,
            String fromText,
            String toText,
            String promotionText) {

        Game game = getGame(gameId);

        if (game.getStatus() != GameStatus.IN_PROGRESS
                && game.getStatus() != GameStatus.CHECK) {
            throw new IllegalStateException("This game has already ended.");
        }

        Player player = findPlayer(game, playerName);

        if (player.getColor() != game.getCurrentTurn()) {
            throw new IllegalArgumentException("It is not your turn.");
        }

        Position from = parsePosition(fromText);
        Position to = parsePosition(toText);

        Piece piece = game.getBoard().getSquare(from).getPiece();

        if (piece == null || piece.getColor() != player.getColor()) {
            throw new IllegalArgumentException(
                    "There is no piece belonging to you on that square.");
        }

        boolean enPassant =
                game.getBoard().isEnPassantMove(from, to);

        Piece capturedPiece = enPassant
                ? game.getBoard().getEnPassantCapturedPiece(from, to)
                : game.getBoard().getSquare(to).getPiece();

        boolean isPromotion =
                piece.getPieceType() == PieceType.PAWN
                        && ((piece.getColor() == Color.WHITE
                        && to.getRow() == 7)
                        || (piece.getColor() == Color.BLACK
                        && to.getRow() == 0));

        PieceType promotionType = null;

        if (isPromotion) {
            promotionType = parsePromotionType(promotionText);
        } else if (promotionText != null && !promotionText.isBlank()) {
            throw new IllegalArgumentException(
                    "Promotion can only be selected when a pawn reaches the last rank.");
        }

        MoveType moveType;

        if (enPassant) {
            moveType = MoveType.EN_PASSANT;
        } else if (isPromotion) {
            moveType = MoveType.PROMOTION;
        } else if (piece.getPieceType() == PieceType.KING
                && Math.abs(from.getColumn() - to.getColumn()) == 2) {
            moveType = MoveType.CASTLING;
        } else if (capturedPiece != null) {
            moveType = MoveType.CAPTURE;
        } else {
            moveType = MoveType.NORMAL;
        }

        Move move = new Move(
                from,
                to,
                piece,
                capturedPiece,
                moveType,
                promotionType
        );

        var previousBoardState = game.getBoard().saveState();

        if (!game.getBoard().executeValidMove(move)) {
            throw new IllegalArgumentException("That move is not allowed.");
        }

        Color timedOutPlayer = game.recordMove(move, previousBoardState);
        if (timedOutPlayer != null) {
            updateRatings(
                    game.getWhitePlayer(),
                    game.getBlackPlayer(),
                    timedOutPlayer == Color.WHITE ? 0.0 : 1.0
            );
            return gameRepository.save(game);
        }

        Color nextTurn = game.getCurrentTurn();
        boolean nextPlayerIsInCheck =
                game.getBoard().isKingInCheck(nextTurn);
        boolean nextPlayerHasMoves =
                game.getBoard().hasAnyLegalMove(nextTurn);

        if (!nextPlayerHasMoves && nextPlayerIsInCheck) {
            game.finish(GameStatus.CHECKMATE);

            double whiteScore = nextTurn == Color.WHITE ? 0.0 : 1.0;
            updateRatings(
                    game.getWhitePlayer(),
                    game.getBlackPlayer(),
                    whiteScore
            );
        } else if (!nextPlayerHasMoves) {
            game.finish(GameStatus.STALEMATE);

            updateRatings(
                    game.getWhitePlayer(),
                    game.getBlackPlayer(),
                    0.5
            );
        } else if (nextPlayerIsInCheck) {
            game.finish(GameStatus.CHECK);
        } else {
            game.finish(GameStatus.IN_PROGRESS);
        }

        return gameRepository.save(game);
    }

    private PieceType parsePromotionType(String promotionText) {
        if (promotionText == null || promotionText.isBlank()) {
            throw new IllegalArgumentException(
                    "Choose a piece for pawn promotion.");
        }

        return switch (promotionText.trim().toLowerCase()) {
            case "q" -> PieceType.QUEEN;
            case "r" -> PieceType.ROOK;
            case "b" -> PieceType.BISHOP;
            case "n" -> PieceType.KNIGHT;
            default -> throw new IllegalArgumentException(
                    "Promotion must be q, r, b, or n.");
        };
    }

    private Position parsePosition(String square) {
        if (square == null || !square.matches("[a-h][1-8]")) {
            throw new IllegalArgumentException(
                    "Square must use chess notation, such as e2 or e4.");
        }

        int column = square.charAt(0) - 'a';
        int row = square.charAt(1) - '1';

        return new Position(row, column);
    }

    public Game resignGame(String gameId, String playerName) {
        Game game = getGame(gameId);

        if (game.getStatus() != GameStatus.IN_PROGRESS
                && game.getStatus() != GameStatus.CHECK) {
            throw new IllegalStateException("This game has already ended.");
        }

        Player resigningPlayer = findPlayer(game, playerName);

        double whiteScore =
                resigningPlayer.getColor() == Color.WHITE ? 0.0 : 1.0;

        updateRatings(
                game.getWhitePlayer(),
                game.getBlackPlayer(),
                whiteScore
        );

        game.finish(GameStatus.RESIGNED);
        return gameRepository.save(game);
    }

    private Player findPlayer(Game game, String playerName) {
        if (playerName == null || playerName.isBlank()) {
            throw new IllegalArgumentException("Player name is required.");
        }

        if (game.getWhitePlayer().getName().equalsIgnoreCase(playerName)) {
            return game.getWhitePlayer();
        }

        if (game.getBlackPlayer().getName().equalsIgnoreCase(playerName)) {
            return game.getBlackPlayer();
        }

        throw new IllegalArgumentException("Player is not in this game.");
    }

    private void updateRatings(
            Player whitePlayer,
            Player blackPlayer,
            double whiteScore) {

        double expectedWhiteScore = 1.0 / (
                1.0 + Math.pow(
                        10,
                        (blackPlayer.getEloRating()
                                - whitePlayer.getEloRating()) / 400.0
                )
        );

        int ratingChange = (int) Math.round(
                32 * (whiteScore - expectedWhiteScore)
        );

        whitePlayer.updateEloRating(ratingChange);
        blackPlayer.updateEloRating(-ratingChange);

        if (accountService != null) {
            accountService.saveRating(
                    whitePlayer.getName(), whitePlayer.getEloRating());
            accountService.saveRating(
                    blackPlayer.getName(), blackPlayer.getEloRating());
        }
    }

    public Game undoLastMove(String gameId, String playerName) {
        Game game = getGame(gameId);

        if (game.getStatus() != GameStatus.IN_PROGRESS
                && game.getStatus() != GameStatus.CHECK) {
            throw new IllegalStateException(
                    "A move cannot be undone after the game has ended.");
        }

        Player player = findPlayer(game, playerName);

        if (game.getMoves().isEmpty()) {
            throw new IllegalStateException("There are no moves to undo.");
        }

        Move lastMove = game.getMoves().get(game.getMoves().size() - 1);
        if (lastMove.getPiece().getColor() != player.getColor()) {
            throw new IllegalArgumentException(
                    "Only the player who made the last move can undo it.");
        }

        game.undoLastMove();
        return gameRepository.save(game);
    }
}
