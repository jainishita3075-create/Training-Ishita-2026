package com.example.ChessGame.model.Entity;

import com.example.ChessGame.model.Enum.Color;
import com.example.ChessGame.model.Enum.MoveType;
import com.example.ChessGame.model.Enum.PieceType;
import com.example.ChessGame.service.PieceFactory;

import java.util.ArrayList;
import java.util.List;

public class Board {

    private final Square[][] squares = new Square[8][8];
    private PieceFactory pieceFactory;

    private boolean whiteKingMoved;
    private boolean blackKingMoved;
    private boolean whiteQueenRookMoved;
    private boolean whiteKingRookMoved;
    private boolean blackQueenRookMoved;
    private boolean blackKingRookMoved;

    private Position enPassantTarget;
    private Position enPassantPawnPosition;
    private Color enPassantPawnColor;

    public Board() {
        initializeBoard();
    }

    private void initializeBoard() {
        for (int row = 0; row < 8; row++) {
            for (int column = 0; column < 8; column++) {
                squares[row][column] =
                        new Square(new Position(row, column));
            }
        }
    }

    public void setupStandardBoard(PieceFactory factory) {
        if (factory == null) {
            throw new IllegalArgumentException("PieceFactory is required.");
        }

        pieceFactory = factory;

        for (int column = 0; column < 8; column++) {
            squares[1][column].setPiece(
                    factory.createPiece(PieceType.PAWN, Color.WHITE));
            squares[6][column].setPiece(
                    factory.createPiece(PieceType.PAWN, Color.BLACK));
        }

        setupBackRow(0, Color.WHITE, factory);
        setupBackRow(7, Color.BLACK, factory);

        resetCastlingRights();
        clearEnPassantOpportunity();
    }

    private void setupBackRow(int row, Color color, PieceFactory factory) {
        squares[row][0].setPiece(factory.createPiece(PieceType.ROOK, color));
        squares[row][1].setPiece(factory.createPiece(PieceType.KNIGHT, color));
        squares[row][2].setPiece(factory.createPiece(PieceType.BISHOP, color));
        squares[row][3].setPiece(factory.createPiece(PieceType.QUEEN, color));
        squares[row][4].setPiece(factory.createPiece(PieceType.KING, color));
        squares[row][5].setPiece(factory.createPiece(PieceType.BISHOP, color));
        squares[row][6].setPiece(factory.createPiece(PieceType.KNIGHT, color));
        squares[row][7].setPiece(factory.createPiece(PieceType.ROOK, color));
    }

    private void resetCastlingRights() {
        whiteKingMoved = false;
        blackKingMoved = false;
        whiteQueenRookMoved = false;
        whiteKingRookMoved = false;
        blackQueenRookMoved = false;
        blackKingRookMoved = false;
    }

    public Square getSquare(Position position) {
        if (position == null || !position.isValid()) {
            throw new IllegalArgumentException("Position must be on the board.");
        }

        return squares[position.getRow()][position.getColumn()];
    }

    public boolean isOccupied(Position position) {
        return !getSquare(position).isEmpty();
    }

    public boolean executeValidMove(Move move) {
        if (move == null || move.getFrom() == null || move.getTo() == null) {
            return false;
        }

        Square source = getSquare(move.getFrom());
        Square target = getSquare(move.getTo());

        if (source.isEmpty()) {
            return false;
        }

        Piece movingPiece = source.getPiece();
        Piece targetPiece = target.getPiece();

        boolean enPassant =
                movingPiece.getPieceType() == PieceType.PAWN
                        && targetPiece == null
                        && isEnPassantMove(move.getFrom(), move.getTo());

        Position capturedPosition = enPassant
                ? enPassantPawnPosition
                : move.getTo();

        Piece capturedPiece = enPassant
                ? getSquare(capturedPosition).getPiece()
                : targetPiece;

        if (capturedPiece != null
                && capturedPiece.getColor() == movingPiece.getColor()) {
            return false;
        }

        // A king is checkmated; it is not captured as a normal move.
        if (capturedPiece != null
                && capturedPiece.getPieceType() == PieceType.KING) {
            return false;
        }

        boolean castling = isCastlingMove(movingPiece, move);
        boolean kingSideCastle = move.getTo().getColumn() == 6;

        if (castling
                && !canCastle(movingPiece.getColor(), kingSideCastle)) {
            return false;
        }

        if (!movingPiece.isValidMove(this, move.getFrom(), move.getTo())) {
            return false;
        }

        Piece promotionPiece = getPromotionPiece(movingPiece, move);

        if (promotionPiece == null
                && isPromotionMove(movingPiece, move.getTo())) {
            return false;
        }

        // Save the board so the trial move can be undone completely.
        BoardState beforeTrial = saveState();

        applyMoveToBoard(
                movingPiece,
                move.getFrom(),
                move.getTo(),
                capturedPosition,
                castling,
                kingSideCastle,
                null
        );

        boolean kingIsSafe = !isKingInCheck(movingPiece.getColor());
        restoreState(beforeTrial);

        if (!kingIsSafe) {
            return false;
        }

        // Apply the actual move.
        applyMoveToBoard(
                movingPiece,
                move.getFrom(),
                move.getTo(),
                capturedPosition,
                castling,
                kingSideCastle,
                promotionPiece
        );

        updateCastlingRights(
                movingPiece,
                move.getFrom(),
                capturedPiece,
                capturedPosition
        );

        updateEnPassantOpportunity(
                movingPiece,
                move.getFrom(),
                move.getTo()
        );

        return true;
    }

    private void applyMoveToBoard(
            Piece movingPiece,
            Position from,
            Position to,
            Position capturedPosition,
            boolean castling,
            boolean kingSideCastle,
            Piece pieceToPlace) {

        int row = from.getRow();

        getSquare(from).setPiece(null);

        if (capturedPosition != null
                && !capturedPosition.equals(to)) {
            getSquare(capturedPosition).setPiece(null);
        }

        getSquare(to).setPiece(
                pieceToPlace == null ? movingPiece : pieceToPlace
        );

        if (castling) {
            int rookFromColumn = kingSideCastle ? 7 : 0;
            int rookToColumn = kingSideCastle ? 5 : 3;

            Piece rook = getSquare(
                    new Position(row, rookFromColumn)
            ).getPiece();

            getSquare(new Position(row, rookFromColumn)).setPiece(null);
            getSquare(new Position(row, rookToColumn)).setPiece(rook);
        }
    }

    private Piece getPromotionPiece(Piece movingPiece, Move move) {
        if (!isPromotionMove(movingPiece, move.getTo())) {
            return movingPiece;
        }

        if (pieceFactory == null) {
            throw new IllegalStateException(
                    "Board must be set up before making moves.");
        }

        PieceType type = move.getPromotionType();

        // If no choice is supplied, promote to a queen.
        if (type == null) {
            type = PieceType.QUEEN;
        }

        if (type != PieceType.QUEEN
                && type != PieceType.ROOK
                && type != PieceType.BISHOP
                && type != PieceType.KNIGHT) {
            return null;
        }

        return pieceFactory.createPiece(type, movingPiece.getColor());
    }

    private boolean isPromotionMove(Piece piece, Position to) {
        return piece.getPieceType() == PieceType.PAWN
                && ((piece.getColor() == Color.WHITE && to.getRow() == 7)
                || (piece.getColor() == Color.BLACK && to.getRow() == 0));
    }

    private boolean isCastlingMove(Piece piece, Move move) {
        return piece.getPieceType() == PieceType.KING
                && move.getFrom().getRow() == move.getTo().getRow()
                && Math.abs(
                move.getFrom().getColumn()
                        - move.getTo().getColumn()) == 2;
    }

    private void updateCastlingRights(
            Piece movingPiece,
            Position from,
            Piece capturedPiece,
            Position capturedPosition) {

        if (movingPiece.getPieceType() == PieceType.KING) {
            if (movingPiece.getColor() == Color.WHITE) {
                whiteKingMoved = true;
            } else {
                blackKingMoved = true;
            }
        }

        if (movingPiece.getPieceType() == PieceType.ROOK) {
            markRookMoved(movingPiece.getColor(), from);
        }

        if (capturedPiece != null
                && capturedPiece.getPieceType() == PieceType.ROOK) {
            markRookMoved(capturedPiece.getColor(), capturedPosition);
        }
    }

    private void markRookMoved(Color color, Position position) {
        int homeRow = color == Color.WHITE ? 0 : 7;

        if (position.getRow() != homeRow) {
            return;
        }

        if (position.getColumn() == 0) {
            if (color == Color.WHITE) {
                whiteQueenRookMoved = true;
            } else {
                blackQueenRookMoved = true;
            }
        } else if (position.getColumn() == 7) {
            if (color == Color.WHITE) {
                whiteKingRookMoved = true;
            } else {
                blackKingRookMoved = true;
            }
        }
    }

    public boolean isEnPassantMove(Position from, Position to) {
        if (from == null || to == null
                || enPassantTarget == null
                || enPassantPawnPosition == null
                || !to.equals(enPassantTarget)
                || !getSquare(to).isEmpty()) {
            return false;
        }

        Piece movingPiece = getSquare(from).getPiece();
        Piece vulnerablePawn =
                getSquare(enPassantPawnPosition).getPiece();

        return movingPiece != null
                && movingPiece.getPieceType() == PieceType.PAWN
                && vulnerablePawn != null
                && vulnerablePawn.getPieceType() == PieceType.PAWN
                && vulnerablePawn.getColor() != movingPiece.getColor()
                && vulnerablePawn.getColor() == enPassantPawnColor
                && Math.abs(from.getColumn() - to.getColumn()) == 1;
    }

    public Piece getEnPassantCapturedPiece(Position from, Position to) {
        if (!isEnPassantMove(from, to)) {
            return null;
        }

        return getSquare(enPassantPawnPosition).getPiece();
    }

    private void updateEnPassantOpportunity(
            Piece movingPiece,
            Position from,
            Position to) {

        clearEnPassantOpportunity();

        if (movingPiece.getPieceType() == PieceType.PAWN
                && Math.abs(from.getRow() - to.getRow()) == 2) {
            int middleRow = (from.getRow() + to.getRow()) / 2;

            enPassantTarget =
                    new Position(middleRow, from.getColumn());
            enPassantPawnPosition = to;
            enPassantPawnColor = movingPiece.getColor();
        }
    }

    private void clearEnPassantOpportunity() {
        enPassantTarget = null;
        enPassantPawnPosition = null;
        enPassantPawnColor = null;
    }

    public List<Position> getCastlingMoves(Color color) {
        List<Position> moves = new ArrayList<>();

        int row = color == Color.WHITE ? 0 : 7;
        Piece king = getSquare(new Position(row, 4)).getPiece();

        if (king == null
                || king.getPieceType() != PieceType.KING
                || king.getColor() != color) {
            return moves;
        }

        if (canCastle(color, true)) {
            moves.add(new Position(row, 6));
        }

        if (canCastle(color, false)) {
            moves.add(new Position(row, 2));
        }

        return moves;
    }

    private boolean canCastle(Color color, boolean kingSide) {
        int row = color == Color.WHITE ? 0 : 7;

        boolean kingMoved =
                color == Color.WHITE ? whiteKingMoved : blackKingMoved;

        boolean rookMoved;
        int rookColumn;

        if (color == Color.WHITE && kingSide) {
            rookMoved = whiteKingRookMoved;
            rookColumn = 7;
        } else if (color == Color.WHITE) {
            rookMoved = whiteQueenRookMoved;
            rookColumn = 0;
        } else if (kingSide) {
            rookMoved = blackKingRookMoved;
            rookColumn = 7;
        } else {
            rookMoved = blackQueenRookMoved;
            rookColumn = 0;
        }

        if (kingMoved || rookMoved || isKingInCheck(color)) {
            return false;
        }

        Piece king = getSquare(new Position(row, 4)).getPiece();
        Piece rook = getSquare(new Position(row, rookColumn)).getPiece();

        if (king == null || king.getPieceType() != PieceType.KING
                || king.getColor() != color
                || rook == null || rook.getPieceType() != PieceType.ROOK
                || rook.getColor() != color) {
            return false;
        }

        int transitColumn = kingSide ? 5 : 3;
        int destinationColumn = kingSide ? 6 : 2;

        if (!getSquare(new Position(row, transitColumn)).isEmpty()
                || !getSquare(new Position(row, destinationColumn)).isEmpty()) {
            return false;
        }

        if (!kingSide && !getSquare(new Position(row, 1)).isEmpty()) {
            return false;
        }

        Color opponent = color == Color.WHITE ? Color.BLACK : Color.WHITE;

        BoardState beforeCheck = saveState();

        Position kingStart = new Position(row, 4);
        Position transit = new Position(row, transitColumn);

        getSquare(kingStart).setPiece(null);
        getSquare(transit).setPiece(king);

        boolean transitIsSafe = !isSquareAttacked(transit, opponent);
        restoreState(beforeCheck);

        if (!transitIsSafe) {
            return false;
        }

        beforeCheck = saveState();

        Position destination = new Position(row, destinationColumn);
        int rookDestinationColumn = kingSide ? 5 : 3;
        Position rookStart = new Position(row, rookColumn);
        Position rookDestination =
                new Position(row, rookDestinationColumn);

        getSquare(kingStart).setPiece(null);
        getSquare(destination).setPiece(king);
        getSquare(rookStart).setPiece(null);
        getSquare(rookDestination).setPiece(rook);

        boolean destinationIsSafe =
                !isSquareAttacked(destination, opponent);

        restoreState(beforeCheck);

        return destinationIsSafe;
    }

    public boolean hasAnyLegalMove(Color color) {
        for (int row = 0; row < 8; row++) {
            for (int column = 0; column < 8; column++) {
                Position from = new Position(row, column);
                Piece piece = getSquare(from).getPiece();

                if (piece == null || piece.getColor() != color) {
                    continue;
                }

                for (Position to : piece.getAvailableMoves(this, from)) {
                    boolean enPassant = isEnPassantMove(from, to);
                    Piece capturedPiece = enPassant
                            ? getEnPassantCapturedPiece(from, to)
                            : getSquare(to).getPiece();

                    MoveType type;

                    if (enPassant) {
                        type = MoveType.EN_PASSANT;
                    } else if (isCastlingMove(
                            piece,
                            new Move(from, to, piece, capturedPiece,
                                    MoveType.NORMAL, null))) {
                        type = MoveType.CASTLING;
                    } else if (capturedPiece != null) {
                        type = MoveType.CAPTURE;
                    } else {
                        type = MoveType.NORMAL;
                    }

                    Move candidate = new Move(
                            from,
                            to,
                            piece,
                            capturedPiece,
                            type,
                            null
                    );

                    BoardState beforeMove = saveState();
                    boolean legal = executeValidMove(candidate);
                    restoreState(beforeMove);

                    if (legal) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private BoardState saveState() {
        Piece[][] pieces = new Piece[8][8];

        for (int row = 0; row < 8; row++) {
            for (int column = 0; column < 8; column++) {
                pieces[row][column] =
                        squares[row][column].getPiece();
            }
        }

        return new BoardState(
                pieces,
                whiteKingMoved,
                blackKingMoved,
                whiteQueenRookMoved,
                whiteKingRookMoved,
                blackQueenRookMoved,
                blackKingRookMoved,
                enPassantTarget,
                enPassantPawnPosition,
                enPassantPawnColor
        );
    }

    private void restoreState(BoardState state) {
        for (int row = 0; row < 8; row++) {
            for (int column = 0; column < 8; column++) {
                squares[row][column].setPiece(
                        state.pieces[row][column]);
            }
        }

        whiteKingMoved = state.whiteKingMoved;
        blackKingMoved = state.blackKingMoved;
        whiteQueenRookMoved = state.whiteQueenRookMoved;
        whiteKingRookMoved = state.whiteKingRookMoved;
        blackQueenRookMoved = state.blackQueenRookMoved;
        blackKingRookMoved = state.blackKingRookMoved;

        enPassantTarget = state.enPassantTarget;
        enPassantPawnPosition = state.enPassantPawnPosition;
        enPassantPawnColor = state.enPassantPawnColor;
    }

    private static class BoardState {
        private final Piece[][] pieces;
        private final boolean whiteKingMoved;
        private final boolean blackKingMoved;
        private final boolean whiteQueenRookMoved;
        private final boolean whiteKingRookMoved;
        private final boolean blackQueenRookMoved;
        private final boolean blackKingRookMoved;
        private final Position enPassantTarget;
        private final Position enPassantPawnPosition;
        private final Color enPassantPawnColor;

        private BoardState(
                Piece[][] pieces,
                boolean whiteKingMoved,
                boolean blackKingMoved,
                boolean whiteQueenRookMoved,
                boolean whiteKingRookMoved,
                boolean blackQueenRookMoved,
                boolean blackKingRookMoved,
                Position enPassantTarget,
                Position enPassantPawnPosition,
                Color enPassantPawnColor) {
            this.pieces = pieces;
            this.whiteKingMoved = whiteKingMoved;
            this.blackKingMoved = blackKingMoved;
            this.whiteQueenRookMoved = whiteQueenRookMoved;
            this.whiteKingRookMoved = whiteKingRookMoved;
            this.blackQueenRookMoved = blackQueenRookMoved;
            this.blackKingRookMoved = blackKingRookMoved;
            this.enPassantTarget = enPassantTarget;
            this.enPassantPawnPosition = enPassantPawnPosition;
            this.enPassantPawnColor = enPassantPawnColor;
        }
    }

    public boolean isKingInCheck(Color kingColor) {
        Position kingPosition = null;

        for (int row = 0; row < 8; row++) {
            for (int column = 0; column < 8; column++) {
                Piece piece = squares[row][column].getPiece();

                if (piece != null
                        && piece.getColor() == kingColor
                        && piece.getPieceType() == PieceType.KING) {
                    kingPosition = new Position(row, column);
                    break;
                }
            }

            if (kingPosition != null) {
                break;
            }
        }

        if (kingPosition == null) {
            return true;
        }

        Color attackingColor =
                kingColor == Color.WHITE ? Color.BLACK : Color.WHITE;

        return isSquareAttacked(kingPosition, attackingColor);
    }

    private boolean isSquareAttacked(
            Position target,
            Color attackingColor) {

        for (int row = 0; row < 8; row++) {
            for (int column = 0; column < 8; column++) {
                Piece piece = squares[row][column].getPiece();

                if (piece == null || piece.getColor() != attackingColor) {
                    continue;
                }

                int rowDifference = target.getRow() - row;
                int columnDifference = target.getColumn() - column;
                int rowDistance = Math.abs(rowDifference);
                int columnDistance = Math.abs(columnDifference);

                PieceType type = piece.getPieceType();

                if (type == PieceType.PAWN) {
                    int direction =
                            attackingColor == Color.WHITE ? 1 : -1;

                    if (rowDifference == direction
                            && columnDistance == 1) {
                        return true;
                    }
                } else if (type == PieceType.KNIGHT
                        && rowDistance * columnDistance == 2) {
                    return true;
                } else if (type == PieceType.KING
                        && Math.max(rowDistance, columnDistance) == 1) {
                    return true;
                } else {
                    boolean straight =
                            rowDifference == 0 || columnDifference == 0;
                    boolean diagonal =
                            rowDistance == columnDistance;

                    boolean matchingDirection =
                            (type == PieceType.ROOK && straight)
                                    || (type == PieceType.BISHOP && diagonal)
                                    || (type == PieceType.QUEEN
                                    && (straight || diagonal));

                    if (matchingDirection
                            && pathIsClear(row, column, target)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private boolean pathIsClear(
            int startRow,
            int startColumn,
            Position target) {

        int rowStep =
                Integer.signum(target.getRow() - startRow);
        int columnStep =
                Integer.signum(target.getColumn() - startColumn);

        int row = startRow + rowStep;
        int column = startColumn + columnStep;

        while (row != target.getRow()
                || column != target.getColumn()) {

            if (!squares[row][column].isEmpty()) {
                return false;
            }

            row += rowStep;
            column += columnStep;
        }

        return true;
    }
}