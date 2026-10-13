package com.example.ChessGame.model.Entity;

import com.example.ChessGame.model.Enum.Color;
import com.example.ChessGame.model.Enum.GameStatus;
import com.example.ChessGame.service.PieceFactory;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Getter
@RequiredArgsConstructor
public class Game {
    private final String id = UUID.randomUUID().toString();
    private final Player whitePlayer;
    private final Player blackPlayer;
    private final Board board = new Board();
    private final List<Move> moves = new ArrayList<>();
    private final List<Board.BoardState> previousBoardStates = new ArrayList<>();
    private final List<GameStatus> previousStatuses = new ArrayList<>();
    private final List<Long> previousWhiteTimes = new ArrayList<>();
    private final List<Long> previousBlackTimes = new ArrayList<>();

    private static final long START_TIME_MILLIS = TimeUnit.MINUTES.toMillis(10);
    private long whiteTimeMillis = START_TIME_MILLIS;
    private long blackTimeMillis = START_TIME_MILLIS;
    private long turnStartedAtMillis = System.currentTimeMillis();

    private Color currentTurn = Color.WHITE;
    private GameStatus status = GameStatus.IN_PROGRESS;

    public Game(Player firstPlayer, Player secondPlayer, PieceFactory pieceFactory) {
        board.setupStandardBoard(pieceFactory);

        if (Math.random() < 0.5) {
            firstPlayer.setColor(Color.WHITE);
            secondPlayer.setColor(Color.BLACK);
            whitePlayer = firstPlayer;
            blackPlayer = secondPlayer;
        } else {
            firstPlayer.setColor(Color.BLACK);
            secondPlayer.setColor(Color.WHITE);
            whitePlayer = secondPlayer;
            blackPlayer = firstPlayer;
        }
    }

    public Color recordMove(Move move, Board.BoardState previousBoardState) {
        Color timedOutPlayer = updateClock();
        if (timedOutPlayer != null) {
            board.restoreState(previousBoardState);
            return timedOutPlayer;
        }

        moves.add(move);
        previousBoardStates.add(previousBoardState);
        previousStatuses.add(status);
        previousWhiteTimes.add(whiteTimeMillis);
        previousBlackTimes.add(blackTimeMillis);
        if (move.getCapturedPiece() != null) {
            getPlayer(move.getPiece().getColor()).addCapturedPiecePoints(
                    move.getCapturedPiece().getPointValue());
        }
        currentTurn = currentTurn == Color.WHITE ? Color.BLACK : Color.WHITE;
        turnStartedAtMillis = System.currentTimeMillis();
        return null;
    }

    public void finish(GameStatus finalStatus) {
        status = finalStatus;
    }

    /** Deducts elapsed time and returns the player who ran out of time, if any. */
    public Color updateClock() {
        if (status != GameStatus.IN_PROGRESS && status != GameStatus.CHECK) {
            return null;
        }

        long now = System.currentTimeMillis();
        long elapsed = now - turnStartedAtMillis;
        long remaining = Math.max(0, getStoredTime(currentTurn) - elapsed);
        setStoredTime(currentTurn, remaining);
        turnStartedAtMillis = now;

        if (remaining == 0) {
            status = currentTurn == Color.WHITE
                    ? GameStatus.WHITE_TIMEOUT
                    : GameStatus.BLACK_TIMEOUT;
            return currentTurn;
        }
        return null;
    }

    public long getWhiteTimeMillis() {
        return getDisplayedTime(Color.WHITE);
    }

    public long getBlackTimeMillis() {
        return getDisplayedTime(Color.BLACK);
    }

    private long getDisplayedTime(Color color) {
        long remaining = getStoredTime(color);
        if ((status == GameStatus.IN_PROGRESS || status == GameStatus.CHECK)
                && currentTurn == color) {
            remaining -= System.currentTimeMillis() - turnStartedAtMillis;
        }
        return Math.max(0, remaining);
    }

    private long getStoredTime(Color color) {
        return color == Color.WHITE ? whiteTimeMillis : blackTimeMillis;
    }

    private void setStoredTime(Color color, long millis) {
        if (color == Color.WHITE) {
            whiteTimeMillis = millis;
        } else {
            blackTimeMillis = millis;
        }
    }

    public void undoLastMove() {
        if (moves.isEmpty()) {
            throw new IllegalStateException("There are no moves to undo.");
        }

        int lastIndex = moves.size() - 1;
        Move lastMove = moves.remove(lastIndex);
        Board.BoardState previousBoardState =
                previousBoardStates.remove(lastIndex);
        long previousWhiteTime = previousWhiteTimes.remove(lastIndex);
        long previousBlackTime = previousBlackTimes.remove(lastIndex);

        board.restoreState(previousBoardState);

        if (lastMove.getCapturedPiece() != null) {
            getPlayer(lastMove.getPiece().getColor()).removeCapturedPiecePoints(
                    lastMove.getCapturedPiece().getPointValue());
        }

        currentTurn = lastMove.getPiece().getColor();
        status = previousStatuses.remove(lastIndex);
        whiteTimeMillis = previousWhiteTime;
        blackTimeMillis = previousBlackTime;
        turnStartedAtMillis = System.currentTimeMillis();
    }

    private Player getPlayer(Color color) {
        return color == Color.WHITE ? whitePlayer : blackPlayer;
    }
}
