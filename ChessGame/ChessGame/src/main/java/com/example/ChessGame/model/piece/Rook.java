package com.example.ChessGame.model.piece;

import com.example.ChessGame.model.Entity.Piece;
import com.example.ChessGame.model.Enum.Color;
import com.example.ChessGame.model.Enum.PieceType;

public class Rook extends Piece {
    public Rook(Color color) {
        super(color, PieceType.ROOK);
    }
}
