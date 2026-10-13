package com.example.ChessGame;

import com.example.ChessGame.model.Entity.Board;
import com.example.ChessGame.model.Entity.Move;
import com.example.ChessGame.model.Entity.Piece;
import com.example.ChessGame.model.Entity.Position;
import com.example.ChessGame.model.Enum.Color;
import com.example.ChessGame.model.Enum.MoveType;
import com.example.ChessGame.model.Enum.PieceType;
import com.example.ChessGame.service.PieceFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BoardRulesTest {

    private Board board;
    private PieceFactory pieceFactory;

    @BeforeEach
    void setUp() {
        board = new Board();
        pieceFactory = new PieceFactory();
        board.setupStandardBoard(pieceFactory);
    }

    @Test
    void whitePawnCanMoveTwoSquaresFromStartingPosition() {
        Position from = new Position(1, 4); // e2
        Position to = new Position(3, 4);   // e4
        Piece pawn = board.getSquare(from).getPiece();

        assertTrue(move(from, to, MoveType.NORMAL, null));
        assertTrue(board.getSquare(from).isEmpty());
        assertSame(pawn, board.getSquare(to).getPiece());
    }

    @Test
    void pawnCannotMoveThreeSquares() {
        Position from = new Position(1, 4); // e2
        Position to = new Position(4, 4);   // e5
        Piece pawn = board.getSquare(from).getPiece();

        assertFalse(move(from, to, MoveType.NORMAL, null));
        assertSame(pawn, board.getSquare(from).getPiece());
        assertTrue(board.getSquare(to).isEmpty());
    }

    @Test
    void pawnCannotMoveForwardIntoOccupiedSquare() {
        Position from = new Position(1, 4); // e2
        Position to = new Position(2, 4);   // e3
        Piece pawn = board.getSquare(from).getPiece();

        board.getSquare(to).setPiece(
                pieceFactory.createPiece(PieceType.ROOK, Color.BLACK)
        );

        assertFalse(move(from, to, MoveType.NORMAL, null));
        assertSame(pawn, board.getSquare(from).getPiece());
    }

    @Test
    void knightCanJumpOverPieces() {
        Position from = new Position(0, 1); // b1
        Position to = new Position(2, 2);   // c3
        Piece knight = board.getSquare(from).getPiece();

        assertTrue(move(from, to, MoveType.NORMAL, null));
        assertSame(knight, board.getSquare(to).getPiece());
    }

    @Test
    void pieceCannotCaptureItsOwnPiece() {
        Position from = new Position(0, 1); // b1, white knight
        Position to = new Position(1, 3);   // d2, white pawn

        assertFalse(move(from, to, MoveType.CAPTURE, null));
        assertEquals(
                PieceType.KNIGHT,
                board.getSquare(from).getPiece().getPieceType()
        );
        assertEquals(
                PieceType.PAWN,
                board.getSquare(to).getPiece().getPieceType()
        );
    }

    @Test
    void rookCannotMoveThroughAnotherPiece() {
        Position from = new Position(0, 0); // a1
        Position to = new Position(3, 0);   // a4

        assertFalse(move(from, to, MoveType.NORMAL, null));
        assertEquals(
                PieceType.ROOK,
                board.getSquare(from).getPiece().getPieceType()
        );
    }

    @Test
    void moveThatExposesOwnKingToCheckIsRejected() {
        clearPieces();

        Position whiteKing = new Position(0, 4); // e1
        Position whiteRook = new Position(1, 4); // e2
        Position blackKing = new Position(7, 0); // a8
        Position blackRook = new Position(7, 4); // e8

        board.getSquare(whiteKing).setPiece(
                pieceFactory.createPiece(PieceType.KING, Color.WHITE)
        );
        board.getSquare(whiteRook).setPiece(
                pieceFactory.createPiece(PieceType.ROOK, Color.WHITE)
        );
        board.getSquare(blackKing).setPiece(
                pieceFactory.createPiece(PieceType.KING, Color.BLACK)
        );
        board.getSquare(blackRook).setPiece(
                pieceFactory.createPiece(PieceType.ROOK, Color.BLACK)
        );

        // Moving the white rook away would expose the white king to the black rook.
        assertFalse(move(
                whiteRook,
                new Position(1, 3), // d2
                MoveType.NORMAL,
                null
        ));

        assertTrue(board.getSquare(whiteKing).getPiece().getPieceType()
                == PieceType.KING);
        assertEquals(
                PieceType.ROOK,
                board.getSquare(whiteRook).getPiece().getPieceType()
        );
    }

    @Test
    void kingCanCastleWhenPathIsClearAndSafe() {
        clearPieces();

        Position whiteKing = new Position(0, 4); // e1
        Position whiteRook = new Position(0, 7); // h1
        Position blackKing = new Position(7, 4); // e8

        board.getSquare(whiteKing).setPiece(
                pieceFactory.createPiece(PieceType.KING, Color.WHITE)
        );
        board.getSquare(whiteRook).setPiece(
                pieceFactory.createPiece(PieceType.ROOK, Color.WHITE)
        );
        board.getSquare(blackKing).setPiece(
                pieceFactory.createPiece(PieceType.KING, Color.BLACK)
        );

        assertTrue(move(
                whiteKing,
                new Position(0, 6), // g1
                MoveType.CASTLING,
                null
        ));

        assertEquals(
                PieceType.KING,
                board.getSquare(new Position(0, 6)).getPiece().getPieceType()
        );
        assertEquals(
                PieceType.ROOK,
                board.getSquare(new Position(0, 5)).getPiece().getPieceType()
        );
    }

    @Test
    void pawnCanCaptureEnPassantAfterOpponentMovesTwoSquares() {
        assertTrue(move(new Position(1, 4), new Position(3, 4),
                MoveType.NORMAL, null));

        assertTrue(move(new Position(6, 0), new Position(5, 0),
                MoveType.NORMAL, null));

        assertTrue(move(new Position(3, 4), new Position(4, 4),
                MoveType.NORMAL, null));

        assertTrue(move(new Position(6, 3), new Position(4, 3),
                MoveType.NORMAL, null));

        assertTrue(move(new Position(4, 4), new Position(5, 3),
                MoveType.EN_PASSANT, null));

        assertTrue(board.getSquare(new Position(4, 3)).isEmpty()); // d5
        assertEquals(
                PieceType.PAWN,
                board.getSquare(new Position(5, 3)).getPiece().getPieceType()
        );
    }

    @Test
    void pawnPromotesToSelectedPiece() {
        clearPieces();

        board.getSquare(new Position(0, 7)).setPiece(
                pieceFactory.createPiece(PieceType.KING, Color.WHITE)
        );
        board.getSquare(new Position(7, 7)).setPiece(
                pieceFactory.createPiece(PieceType.KING, Color.BLACK)
        );
        board.getSquare(new Position(6, 0)).setPiece(
                pieceFactory.createPiece(PieceType.PAWN, Color.WHITE)
        );

        assertTrue(move(
                new Position(6, 0), // a7
                new Position(7, 0), // a8
                MoveType.PROMOTION,
                PieceType.KNIGHT
        ));

        assertEquals(
                PieceType.KNIGHT,
                board.getSquare(new Position(7, 0)).getPiece().getPieceType()
        );
    }

    @Test
    void kingCannotCastleThroughAnAttackedSquare() {
        clearPieces();

        Position whiteKing = new Position(0, 4); // e1
        Position whiteRook = new Position(0, 7); // h1
        Position blackKing = new Position(7, 0); // a8
        Position blackRook = new Position(7, 5); // f8 attacks f1

        board.getSquare(whiteKing).setPiece(
                pieceFactory.createPiece(PieceType.KING, Color.WHITE)
        );
        board.getSquare(whiteRook).setPiece(
                pieceFactory.createPiece(PieceType.ROOK, Color.WHITE)
        );
        board.getSquare(blackKing).setPiece(
                pieceFactory.createPiece(PieceType.KING, Color.BLACK)
        );
        board.getSquare(blackRook).setPiece(
                pieceFactory.createPiece(PieceType.ROOK, Color.BLACK)
        );

        assertFalse(move(
                whiteKing,
                new Position(0, 6), // g1
                MoveType.CASTLING,
                null
        ));

        assertEquals(
                PieceType.KING,
                board.getSquare(whiteKing).getPiece().getPieceType()
        );
        assertEquals(
                PieceType.ROOK,
                board.getSquare(whiteRook).getPiece().getPieceType()
        );
    }

    @Test
    void kingCannotCastleIfRookMovedAndReturned() {
        clearPieces();

        Position whiteKing = new Position(0, 4); // e1
        Position rookHome = new Position(0, 7);  // h1
        Position rookNext = new Position(1, 7);  // h2

        board.getSquare(whiteKing).setPiece(
                pieceFactory.createPiece(PieceType.KING, Color.WHITE)
        );
        board.getSquare(rookHome).setPiece(
                pieceFactory.createPiece(PieceType.ROOK, Color.WHITE)
        );
        board.getSquare(new Position(7, 4)).setPiece(
                pieceFactory.createPiece(PieceType.KING, Color.BLACK)
        );

        assertTrue(move(rookHome, rookNext, MoveType.NORMAL, null));
        assertTrue(move(rookNext, rookHome, MoveType.NORMAL, null));

        assertFalse(move(
                whiteKing,
                new Position(0, 6), // g1
                MoveType.CASTLING,
                null
        ));
    }

    @Test
    void enPassantOpportunityExpiresAfterAnotherMove() {
        assertTrue(move(new Position(1, 4), new Position(3, 4),
                MoveType.NORMAL, null));

        assertTrue(move(new Position(6, 0), new Position(5, 0),
                MoveType.NORMAL, null));

        assertTrue(move(new Position(3, 4), new Position(4, 4),
                MoveType.NORMAL, null));

        assertTrue(move(new Position(6, 3), new Position(4, 3),
                MoveType.NORMAL, null));
        assertTrue(board.isEnPassantMove(
                new Position(4, 4),
                new Position(5, 3)
        ));

        assertTrue(move(new Position(0, 1), new Position(2, 2),
                MoveType.NORMAL, null)); // Nb1-c3

        assertFalse(board.isEnPassantMove(
                new Position(4, 4),
                new Position(5, 3)
        ));
    }

    @Test
    void detectsStalematePosition() {
        for (int row = 0; row < 8; row++) {
            for (int column = 0; column < 8; column++) {
                board.getSquare(new Position(row, column)).setPiece(null);
            }
        }

        board.getSquare(new Position(7, 0))
                .setPiece(pieceFactory.createPiece(PieceType.KING, Color.BLACK));
        board.getSquare(new Position(5, 2))
                .setPiece(pieceFactory.createPiece(PieceType.KING, Color.WHITE));
        board.getSquare(new Position(5, 1))
                .setPiece(pieceFactory.createPiece(PieceType.QUEEN, Color.WHITE));

        assertFalse(board.isKingInCheck(Color.BLACK));
        assertFalse(board.hasAnyLegalMove(Color.BLACK));
    }

    private boolean move(
            Position from,
            Position to,
            MoveType moveType,
            PieceType promotionType) {

        Piece movingPiece = board.getSquare(from).getPiece();
        Piece capturedPiece = board.getSquare(to).getPiece();

        if (movingPiece == null) {
            return false;
        }

        Move move = new Move(
                from,
                to,
                movingPiece,
                capturedPiece,
                moveType,
                promotionType
        );

        return board.executeValidMove(move);
    }

    private void clearPieces() {
        for (int row = 0; row < 8; row++) {
            for (int column = 0; column < 8; column++) {
                board.getSquare(new Position(row, column)).setPiece(null);
            }
        }
    }
}