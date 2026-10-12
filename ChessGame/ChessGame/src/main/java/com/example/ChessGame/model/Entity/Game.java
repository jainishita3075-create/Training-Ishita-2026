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

    public void recordMove(Move move) {
        moves.add(move);
        currentTurn = currentTurn == Color.WHITE ? Color.BLACK : Color.WHITE;
    }

    public void finish(GameStatus finalStatus) {
        status = finalStatus;
    }
}