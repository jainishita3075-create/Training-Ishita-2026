package com.example.ChessGame.service;

import com.example.ChessGame.model.Entity.Piece;
import com.example.ChessGame.model.Enum.Color;
import com.example.ChessGame.model.Enum.PieceType;
import com.example.ChessGame.model.piece.*;

public class PieceFactory {
    public Piece createPiece(PieceType type, Color color) {
        if (type == null || color == null) {
            throw new IllegalArgumentException("Piece type and color are required.");
        }

        return switch (type) {
            case KING -> new King(color);
            case QUEEN -> new Queen(color);
            case ROOK -> new Rook(color);
            case BISHOP -> new Bishop(color);
            case KNIGHT -> new Knight(color);
            case PAWN -> new Pawn(color);
        };
    }
}