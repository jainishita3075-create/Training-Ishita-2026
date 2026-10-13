package com.example.ChessGame.model.rules;

import com.example.ChessGame.model.Entity.Piece;
import com.example.ChessGame.model.Entity.Position;
import com.example.ChessGame.model.Enum.Color;
import com.example.ChessGame.model.Enum.PieceType;
import com.example.ChessGame.service.PieceFactory;

public class PromotionRule {

    public boolean appliesTo(Piece piece, Position destination) {
        return piece.getPieceType() == PieceType.PAWN
                && ((piece.getColor() == Color.WHITE && destination.getRow() == 7)
                || (piece.getColor() == Color.BLACK && destination.getRow() == 0));
    }

    public Piece promote(PieceFactory factory, Piece pawn, PieceType choice) {
        PieceType type = choice == null ? PieceType.QUEEN : choice;
        if (type != PieceType.QUEEN && type != PieceType.ROOK
                && type != PieceType.BISHOP && type != PieceType.KNIGHT) {
            return null;
        }
        return factory.createPiece(type, pawn.getColor());
    }
}
