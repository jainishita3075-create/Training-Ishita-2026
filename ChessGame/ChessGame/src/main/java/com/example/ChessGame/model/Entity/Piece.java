package com.example.ChessGame.model.Entity;

import com.example.ChessGame.model.Enum.Color;
import com.example.ChessGame.model.Enum.PieceType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@RequiredArgsConstructor
public abstract class Piece {
    protected final Color color;
    protected final PieceType pieceType;

    public boolean isCapture(Piece targetPiece){
        return targetPiece != null && targetPiece.getColor() != this.color;
//  checks if the piece is captured or not and if piece is of opponent or not
    }
    public abstract boolean isValidMove(Board board, Position from, Position to);
    public abstract List<Position> getAvailableMoves(Board board, Position from);

//  score system
    public int getPointValue() {
        return switch (pieceType) {
            case PAWN -> 1;
            case KNIGHT, BISHOP -> 3;
            case ROOK -> 5;
            case QUEEN -> 9;
            case KING -> 0;     // checkmate
        };
    }

    protected List<Position> getSlidingMoves(
            Board board,
            Position from,
            int[][] directions) {

        List<Position> moves = new ArrayList<>();

        for (int[] direction : directions) {
            int row = from.getRow() + direction[0];
            int column = from.getColumn() + direction[1];

            while (row >= 0 && row < 8
                    && column >= 0 && column < 8) {

                Position next = new Position(row, column);
                var square = board.getSquare(next);

                if (square.isEmpty()) {
                    moves.add(next);
                } else {
                    if (isCapture(square.getPiece())) {
                        moves.add(next);
                    }
                    break;
                }

                row += direction[0];
                column += direction[1];
            }
        }

        return moves;
    }
}
