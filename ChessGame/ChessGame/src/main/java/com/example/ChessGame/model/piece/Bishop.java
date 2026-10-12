package com.example.ChessGame.model.piece;

import com.example.ChessGame.model.Entity.Board;
import com.example.ChessGame.model.Entity.Piece;
import com.example.ChessGame.model.Entity.Position;
import com.example.ChessGame.model.Enum.Color;
import com.example.ChessGame.model.Enum.PieceType;

import java.util.List;

public class Bishop extends Piece {
    public Bishop(Color color) {
        super(color, PieceType.BISHOP);
    }

    @Override
    public boolean isValidMove(Board board, Position from, Position to) {
        return getAvailableMoves(board, from).contains(to);
    }

    @Override
    public List<Position> getAvailableMoves(Board board, Position from) {
//      diagonal movements
        int[][] directions = {
                {-1, -1},
                {-1, 1},
                {1, -1},
                {1, 1}
        };
        return getSlidingMoves(board, from, directions);
    }
}
