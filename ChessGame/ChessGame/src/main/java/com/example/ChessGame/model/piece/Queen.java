package com.example.ChessGame.model.piece;

import com.example.ChessGame.model.Entity.Board;
import com.example.ChessGame.model.Entity.Piece;
import com.example.ChessGame.model.Entity.Position;
import com.example.ChessGame.model.Enum.Color;
import com.example.ChessGame.model.Enum.PieceType;

import java.util.List;

public class Queen extends Piece {
    public Queen(Color color) {
        super(color, PieceType.QUEEN);
    }

    @Override
    public boolean isValidMove(Board board, Position from, Position to) {
        return getAvailableMoves(board, from).contains(to);
    }

    @Override
    public List<Position> getAvailableMoves(Board board, Position from) {
        int[][] directions = {
                {-1, 0},  // Up
                { 1, 0},  // Down
                { 0,-1},  // Left
                { 0, 1},  // Right

                {-1,-1},  // Diagonal up-left
                {-1, 1},  // Diagonal up-right
                { 1,-1},  // Diagonal down-left
                { 1, 1}   // Diagonal down-right
        };

        return getSlidingMoves(board, from, directions);
    }
}
