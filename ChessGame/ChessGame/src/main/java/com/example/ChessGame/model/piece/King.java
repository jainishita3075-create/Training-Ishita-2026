package com.example.ChessGame.model.piece;

import com.example.ChessGame.model.Entity.Piece;
import com.example.ChessGame.model.Enum.Color;
import com.example.ChessGame.model.Enum.PieceType;

public class King extends Piece {
    public King(Color color) {
        super(color, PieceType.KING);
    }
}
