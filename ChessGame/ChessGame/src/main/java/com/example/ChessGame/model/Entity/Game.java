package com.example.ChessGame.model.Entity;

import com.example.ChessGame.model.Enum.Color;
import com.example.ChessGame.model.Enum.GameStatus;
import com.example.ChessGame.service.PieceFactory;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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

    public void recordMove(Move move, Board.BoardState previousBoardState) {
        moves.add(move);
        previousBoardStates.add(previousBoardState);
        previousStatuses.add(status);
        if (move.getCapturedPiece() != null) {
            getPlayer(move.getPiece().getColor()).addCapturedPiecePoints(
                    move.getCapturedPiece().getPointValue());
        }
        currentTurn = currentTurn == Color.WHITE ? Color.BLACK : Color.WHITE;
    }

    public void finish(GameStatus finalStatus) {
        status = finalStatus;
    }

    public void undoLastMove() {
        if (moves.isEmpty()) {
            throw new IllegalStateException("There are no moves to undo.");
        }

        int lastIndex = moves.size() - 1;
        Move lastMove = moves.remove(lastIndex);
        Board.BoardState previousBoardState =
                previousBoardStates.remove(lastIndex);

        board.restoreState(previousBoardState);

        if (lastMove.getCapturedPiece() != null) {
            getPlayer(lastMove.getPiece().getColor()).removeCapturedPiecePoints(
                    lastMove.getCapturedPiece().getPointValue());
        }

        currentTurn = lastMove.getPiece().getColor();
        status = previousStatuses.remove(lastIndex);
    }

    private Player getPlayer(Color color) {
        return color == Color.WHITE ? whitePlayer : blackPlayer;
    }
}
