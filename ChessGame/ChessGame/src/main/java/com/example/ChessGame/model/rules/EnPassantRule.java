package com.example.ChessGame.model.rules;

import com.example.ChessGame.model.Entity.Board;
import com.example.ChessGame.model.Entity.Piece;
import com.example.ChessGame.model.Entity.Position;
import com.example.ChessGame.model.Enum.Color;
import com.example.ChessGame.model.Enum.PieceType;

/** Validates whether a pawn may capture en passant in the current position. */
public class EnPassantRule {

    public boolean isLegal(
            Board board,
            Position from,
            Position to,
            Position target,
            Position vulnerablePawnPosition,
            Color vulnerablePawnColor) {

        if (from == null || to == null || target == null
                || vulnerablePawnPosition == null
                || !from.isValid() || !to.isValid()
                || !to.equals(target) || board.isOccupied(to)) {
            return false;
        }

        Piece movingPiece = board.getSquare(from).getPiece();
        Piece vulnerablePawn =
                board.getSquare(vulnerablePawnPosition).getPiece();

        if (movingPiece == null || movingPiece.getPieceType() != PieceType.PAWN
                || vulnerablePawn == null
                || vulnerablePawn.getPieceType() != PieceType.PAWN
                || vulnerablePawn.getColor() == movingPiece.getColor()
                || vulnerablePawn.getColor() != vulnerablePawnColor) {
            return false;
        }

        int direction = movingPiece.getColor() == Color.WHITE ? 1 : -1;
        return to.getRow() - from.getRow() == direction
                && Math.abs(to.getColumn() - from.getColumn()) == 1
                && vulnerablePawnPosition.getRow() == from.getRow()
                && vulnerablePawnPosition.getColumn() == to.getColumn();
    }
}
