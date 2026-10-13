package com.example.ChessGame.model.rules;

import com.example.ChessGame.model.Entity.Board;
import com.example.ChessGame.model.Entity.Piece;
import com.example.ChessGame.model.Entity.Position;
import com.example.ChessGame.model.Enum.Color;
import com.example.ChessGame.model.Enum.PieceType;

public class CheckRule {

    public boolean isKingInCheck(Board board, Color kingColor) {
        for (int row = 0; row < 8; row++) {
            for (int column = 0; column < 8; column++) {
                Piece piece = board.getSquare(new Position(row, column)).getPiece();
                if (piece != null && piece.getColor() == kingColor
                        && piece.getPieceType() == PieceType.KING) {
                    Color attacker = kingColor == Color.WHITE
                            ? Color.BLACK : Color.WHITE;
                    return isSquareAttacked(
                            board, new Position(row, column), attacker);
                }
            }
        }
        return true;
    }

    public boolean isSquareAttacked(
            Board board, Position target, Color attackingColor) {
        for (int row = 0; row < 8; row++) {
            for (int column = 0; column < 8; column++) {
                Piece piece = board.getSquare(new Position(row, column)).getPiece();
                if (piece == null || piece.getColor() != attackingColor) {
                    continue;
                }

                int rowDifference = target.getRow() - row;
                int columnDifference = target.getColumn() - column;
                int rowDistance = Math.abs(rowDifference);
                int columnDistance = Math.abs(columnDifference);
                PieceType type = piece.getPieceType();

                if (type == PieceType.PAWN) {
                    int direction = attackingColor == Color.WHITE ? 1 : -1;
                    if (rowDifference == direction && columnDistance == 1) {
                        return true;
                    }
                } else if (type == PieceType.KNIGHT
                        && rowDistance * columnDistance == 2) {
                    return true;
                } else if (type == PieceType.KING
                        && Math.max(rowDistance, columnDistance) == 1) {
                    return true;
                } else if (attacksInLine(type, rowDifference, columnDifference)
                        && pathIsClear(board, row, column, target)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean attacksInLine(
            PieceType type, int rowDifference, int columnDifference) {
        boolean straight = rowDifference == 0 || columnDifference == 0;
        boolean diagonal = Math.abs(rowDifference) == Math.abs(columnDifference);
        return (type == PieceType.ROOK && straight)
                || (type == PieceType.BISHOP && diagonal)
                || (type == PieceType.QUEEN && (straight || diagonal));
    }

    private boolean pathIsClear(
            Board board, int startRow, int startColumn, Position target) {
        int rowStep = Integer.signum(target.getRow() - startRow);
        int columnStep = Integer.signum(target.getColumn() - startColumn);
        int row = startRow + rowStep;
        int column = startColumn + columnStep;

        while (row != target.getRow() || column != target.getColumn()) {
            if (board.isOccupied(new Position(row, column))) {
                return false;
            }
            row += rowStep;
            column += columnStep;
        }
        return true;
    }
}
