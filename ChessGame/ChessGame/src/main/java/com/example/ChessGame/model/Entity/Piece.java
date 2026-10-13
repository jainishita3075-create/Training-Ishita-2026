package com.example.ChessGame.model.Entity;

import com.example.ChessGame.model.Enum.Color;
import com.example.ChessGame.model.Enum.PieceType;
import com.example.ChessGame.model.rules.PieceMovementRule;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public abstract class Piece {
    private static final PieceMovementRule MOVEMENT_RULE = new PieceMovementRule();
    protected final Color color;
    protected final PieceType pieceType;

    public boolean isCapture(Piece targetPiece){
        return targetPiece != null && targetPiece.getColor() != this.color;
//  checks if the piece is captured or not and if piece is of opponent or not
    }
    public boolean isValidMove(Board board, Position from, Position to) {
        return getAvailableMoves(board, from).contains(to);
    }

    public List<Position> getAvailableMoves(Board board, Position from) {
        return MOVEMENT_RULE.getMoves(board, this, from);
    }

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

}
