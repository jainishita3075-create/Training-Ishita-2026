package com.example.ChessGame.model.Entity;

import com.example.ChessGame.model.Enum.Color;
import com.example.ChessGame.model.Enum.MoveType;
import com.example.ChessGame.model.Enum.PieceType;
import com.example.ChessGame.service.PieceFactory;
import com.example.ChessGame.model.rules.CastlingRule;
import com.example.ChessGame.model.rules.CheckRule;
import com.example.ChessGame.model.rules.EnPassantRule;
import com.example.ChessGame.model.rules.PromotionRule;

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
    private final EnPassantRule enPassantRule = new EnPassantRule();
    private final CastlingRule castlingRule = new CastlingRule();
    private final CheckRule checkRule = new CheckRule();
    private final PromotionRule promotionRule = new PromotionRule();

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

        boolean castling = castlingRule.isCastlingMove(
                movingPiece, move.getFrom(), move.getTo());
        boolean kingSideCastle = move.getTo().getColumn() == 6;

        if (castling
                && !canCastle(movingPiece.getColor(), kingSideCastle)) {
            return false;
        }

        if (!movingPiece.isValidMove(this, move.getFrom(), move.getTo())) {
            return false;
        }

        Piece promotionPiece = getPromotionPiece(movingPiece, move);

        if (promotionPiece == null && promotionRule.appliesTo(movingPiece, move.getTo())) {
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
        if (!promotionRule.appliesTo(movingPiece, move.getTo())) {
            return movingPiece;
        }

        if (pieceFactory == null) {
            throw new IllegalStateException(
                    "Board must be set up before making moves.");
        }

        return promotionRule.promote(
                pieceFactory, movingPiece, move.getPromotionType());
    }

    private boolean isCastlingMove(Piece piece, Move move) {
        return castlingRule.isCastlingMove(
                piece, move.getFrom(), move.getTo());
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
        return enPassantRule.isLegal(
                this, from, to, enPassantTarget,
                enPassantPawnPosition, enPassantPawnColor);
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
        boolean kingMoved = color == Color.WHITE
                ? whiteKingMoved : blackKingMoved;
        boolean rookMoved;

        if (color == Color.WHITE) {
            rookMoved = kingSide ? whiteKingRookMoved : whiteQueenRookMoved;
        } else {
            rookMoved = kingSide ? blackKingRookMoved : blackQueenRookMoved;
        }

        return castlingRule.canCastle(
                this, color, kingSide, kingMoved, rookMoved);
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

    public BoardState saveState() {
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

    public void restoreState(BoardState state) {
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

    public static class BoardState {
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
        return checkRule.isKingInCheck(this, kingColor);
    }
}
