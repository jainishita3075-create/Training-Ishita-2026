package com.example.ChessGame.model.piece;

import com.example.ChessGame.model.Entity.Board;
import com.example.ChessGame.model.Entity.Piece;
import com.example.ChessGame.model.Entity.Position;
import com.example.ChessGame.model.Enum.Color;
import com.example.ChessGame.model.Enum.PieceType;

import java.util.ArrayList;
import java.util.List;

public class Knight extends Piece {

    public Knight(Color color) {
        super(color, PieceType.KNIGHT);
    }

    @Override
    public boolean isValidMove(Board board, Position from, Position to) {
        return getAvailableMoves(board, from).contains(to);
    }

    @Override
    public List<Position> getAvailableMoves(Board board, Position from) {
        List<Position> moves = new ArrayList<>();

//        L-shape movement
        int[][] offsets = {
                {-2, -1},
                {-2, 1},
                {-1, -2},
                {-1, 2},
                {1, -2},
                {1, 2},
                {2, -1},
                {2, 1}
        };

        for (int[] offset : offsets) {
            Position target = new Position(
                    from.getRow() + offset[0],
                    from.getColumn() + offset[1]
            );

            if (target.isValid()) {
                Piece piece = board.getSquare(target).getPiece();

                if (piece == null || piece.getColor() != color) {
                    moves.add(target);
                }
            }
        }

        return moves;
    }
}