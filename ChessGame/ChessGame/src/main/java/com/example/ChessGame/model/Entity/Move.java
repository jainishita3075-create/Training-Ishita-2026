package com.example.ChessGame.model.Entity;

import com.example.ChessGame.model.Enum.PieceType;
import com.example.ChessGame.model.Enum.MoveType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class Move {
    private final Position from;
    private final Position to;
    private final Piece piece;
    private final Piece capturedPiece;
    private final MoveType moveType;
    private final PieceType promotionType;

}

// also helpful to maintain history, in undo operation
