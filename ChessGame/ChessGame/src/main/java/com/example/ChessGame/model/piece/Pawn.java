package com.example.ChessGame.model.piece;

import com.example.ChessGame.model.Entity.Board;
import com.example.ChessGame.model.Entity.Piece;
import com.example.ChessGame.model.Entity.Position;
import com.example.ChessGame.model.Enum.Color;
import com.example.ChessGame.model.Enum.PieceType;

import java.util.ArrayList;
import java.util.List;

public class Pawn extends Piece {

    public Pawn(Color color) {
        super(color, PieceType.PAWN);
    }

    @Override
    public boolean isValidMove(Board board, Position from, Position to) {
        return getAvailableMoves(board, from).contains(to);
    }

    @Override
    public List<Position> getAvailableMoves(Board board, Position from) {
        List<Position> moves = new ArrayList<>();

        int direction = color == Color.WHITE ? 1 : -1;
        int startingRow = color == Color.WHITE ? 1 : 6;

        Position oneStep = new Position(
                from.getRow() + direction,
                from.getColumn()
        );

        if (oneStep.isValid() && !board.isOccupied(oneStep)) {
            moves.add(oneStep);

            Position twoSteps = new Position(
                    from.getRow() + 2 * direction,
                    from.getColumn()
            );

            if (from.getRow() == startingRow
                    && twoSteps.isValid()
                    && !board.isOccupied(twoSteps)) {
                moves.add(twoSteps);
            }
        }

        for (int columnOffset : new int[]{-1, 1}) {
            Position diagonal = new Position(
                    from.getRow() + direction,
                    from.getColumn() + columnOffset
            );

            if (!diagonal.isValid()) {
                continue;
            }

            Piece targetPiece = board.getSquare(diagonal).getPiece();

            if (isCapture(targetPiece)
                    || board.isEnPassantMove(from, diagonal)) {
                moves.add(diagonal);
            }
        }

        return moves;
    }
}