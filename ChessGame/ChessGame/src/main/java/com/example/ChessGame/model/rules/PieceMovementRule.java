package com.example.ChessGame.model.rules;

import com.example.ChessGame.model.Entity.Board;
import com.example.ChessGame.model.Entity.Piece;
import com.example.ChessGame.model.Entity.Position;
import com.example.ChessGame.model.Enum.Color;
import com.example.ChessGame.model.Enum.PieceType;

import java.util.ArrayList;
import java.util.List;

/** Generates possible destinations for each piece; Board checks full legality. */
public class PieceMovementRule {

    public List<Position> getMoves(Board board, Piece piece, Position from) {
        return switch (piece.getPieceType()) {
            case PAWN -> pawnMoves(board, piece, from);
            case KNIGHT -> knightMoves(board, piece, from);
            case KING -> kingMoves(board, piece, from);
            case ROOK -> slidingMoves(board, piece, from,
                    new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}});
            case BISHOP -> slidingMoves(board, piece, from,
                    new int[][]{{1, 1}, {1, -1}, {-1, 1}, {-1, -1}});
            case QUEEN -> slidingMoves(board, piece, from,
                    new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1},
                            {1, 1}, {1, -1}, {-1, 1}, {-1, -1}});
        };
    }

    private List<Position> pawnMoves(Board board, Piece piece, Position from) {
        List<Position> moves = new ArrayList<>();
        int direction = piece.getColor() == Color.WHITE ? 1 : -1;
        int startRow = piece.getColor() == Color.WHITE ? 1 : 6;
        Position one = new Position(from.getRow() + direction, from.getColumn());

        if (one.isValid() && !board.isOccupied(one)) {
            moves.add(one);
            Position two = new Position(from.getRow() + 2 * direction, from.getColumn());
            if (from.getRow() == startRow && two.isValid() && !board.isOccupied(two)) {
                moves.add(two);
            }
        }

        for (int offset : new int[]{-1, 1}) {
            Position diagonal = new Position(
                    from.getRow() + direction, from.getColumn() + offset);
            if (diagonal.isValid()) {
                Piece target = board.getSquare(diagonal).getPiece();
                if (piece.isCapture(target) || board.isEnPassantMove(from, diagonal)) {
                    moves.add(diagonal);
                }
            }
        }
        return moves;
    }

    private List<Position> knightMoves(Board board, Piece piece, Position from) {
        List<Position> moves = new ArrayList<>();
        int[][] offsets = {{-2, -1}, {-2, 1}, {-1, -2}, {-1, 2},
                {1, -2}, {1, 2}, {2, -1}, {2, 1}};
        for (int[] offset : offsets) {
            addIfAvailable(board, piece, moves,
                    from.getRow() + offset[0], from.getColumn() + offset[1]);
        }
        return moves;
    }

    private List<Position> kingMoves(Board board, Piece piece, Position from) {
        List<Position> moves = new ArrayList<>();
        for (int rowOffset = -1; rowOffset <= 1; rowOffset++) {
            for (int columnOffset = -1; columnOffset <= 1; columnOffset++) {
                if (rowOffset != 0 || columnOffset != 0) {
                    addIfAvailable(board, piece, moves,
                            from.getRow() + rowOffset,
                            from.getColumn() + columnOffset);
                }
            }
        }
        moves.addAll(board.getCastlingMoves(piece.getColor()));
        return moves;
    }

    private List<Position> slidingMoves(
            Board board, Piece piece, Position from, int[][] directions) {
        List<Position> moves = new ArrayList<>();
        for (int[] direction : directions) {
            int row = from.getRow() + direction[0];
            int column = from.getColumn() + direction[1];
            while (row >= 0 && row < 8 && column >= 0 && column < 8) {
                Position target = new Position(row, column);
                Piece targetPiece = board.getSquare(target).getPiece();
                if (targetPiece == null) {
                    moves.add(target);
                } else {
                    if (piece.isCapture(targetPiece)) moves.add(target);
                    break;
                }
                row += direction[0];
                column += direction[1];
            }
        }
        return moves;
    }

    private void addIfAvailable(
            Board board, Piece piece, List<Position> moves, int row, int column) {
        Position target = new Position(row, column);
        if (!target.isValid()) return;
        Piece targetPiece = board.getSquare(target).getPiece();
        if (targetPiece == null || piece.isCapture(targetPiece)) moves.add(target);
    }
}
