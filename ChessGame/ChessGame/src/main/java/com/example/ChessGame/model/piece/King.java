package com.example.ChessGame.model.piece;

import com.example.ChessGame.model.Entity.Board;
import com.example.ChessGame.model.Entity.Piece;
import com.example.ChessGame.model.Entity.Position;
import com.example.ChessGame.model.Enum.Color;
import com.example.ChessGame.model.Enum.PieceType;

import java.util.ArrayList;
import java.util.List;

public class King extends Piece {

    public King(Color color) {
        super(color, PieceType.KING);
    }

    @Override
    public boolean isValidMove(Board board, Position from, Position to) {
        return getAvailableMoves(board, from).contains(to);
    }

    @Override
    public List<Position> getAvailableMoves(Board board, Position from) {
        List<Position> moves = new ArrayList<>();

        int[][] directions = {
                {-1, -1}, {-1, 0}, {-1, 1},
                {0, -1},            {0, 1},
                {1, -1},  {1, 0},   {1, 1}
        };

        for (int[] direction : directions) {
            Position target = new Position(
                    from.getRow() + direction[0],
                    from.getColumn() + direction[1]
            );

            if (!target.isValid()) {
                continue;
            }

            var square = board.getSquare(target);

            if (square.isEmpty() || isCapture(square.getPiece())) {
                moves.add(target);
            }
        }

        moves.addAll(board.getCastlingMoves(color));

        return moves;
    }
}