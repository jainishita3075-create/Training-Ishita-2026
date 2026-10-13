package com.example.ChessGame.model.rules;

import com.example.ChessGame.model.Entity.Board;
import com.example.ChessGame.model.Entity.Piece;
import com.example.ChessGame.model.Entity.Position;
import com.example.ChessGame.model.Enum.Color;
import com.example.ChessGame.model.Enum.PieceType;

public class CastlingRule {
    private final CheckRule checkRule = new CheckRule();

    public boolean isCastlingMove(Piece piece, Position from, Position to) {
        return piece.getPieceType() == PieceType.KING
                && from.getRow() == to.getRow()
                && Math.abs(from.getColumn() - to.getColumn()) == 2;
    }

    public boolean canCastle(
            Board board,
            Color color,
            boolean kingSide,
            boolean kingMoved,
            boolean rookMoved) {

        int row = color == Color.WHITE ? 0 : 7;
        int rookColumn = kingSide ? 7 : 0;
        int transitColumn = kingSide ? 5 : 3;
        int destinationColumn = kingSide ? 6 : 2;

        if (kingMoved || rookMoved || checkRule.isKingInCheck(board, color)) {
            return false;
        }

        Piece king = board.getSquare(new Position(row, 4)).getPiece();
        Piece rook = board.getSquare(new Position(row, rookColumn)).getPiece();
        if (king == null || king.getPieceType() != PieceType.KING
                || king.getColor() != color
                || rook == null || rook.getPieceType() != PieceType.ROOK
                || rook.getColor() != color) {
            return false;
        }

        if (!board.getSquare(new Position(row, transitColumn)).isEmpty()
                || !board.getSquare(new Position(row, destinationColumn)).isEmpty()) {
            return false;
        }
        if (!kingSide && !board.getSquare(new Position(row, 1)).isEmpty()) {
            return false;
        }

        Color opponent = color == Color.WHITE ? Color.BLACK : Color.WHITE;
        Board.BoardState before = board.saveState();
        Position start = new Position(row, 4);
        Position transit = new Position(row, transitColumn);
        board.getSquare(start).setPiece(null);
        board.getSquare(transit).setPiece(king);
        boolean transitSafe = !checkRule.isSquareAttacked(board, transit, opponent);
        board.restoreState(before);
        if (!transitSafe) {
            return false;
        }

        before = board.saveState();
        Position destination = new Position(row, destinationColumn);
        Position rookStart = new Position(row, rookColumn);
        Position rookDestination = new Position(row, kingSide ? 5 : 3);
        board.getSquare(start).setPiece(null);
        board.getSquare(destination).setPiece(king);
        board.getSquare(rookStart).setPiece(null);
        board.getSquare(rookDestination).setPiece(rook);
        boolean destinationSafe =
                !checkRule.isSquareAttacked(board, destination, opponent);
        board.restoreState(before);
        return destinationSafe;
    }
}
