package com.example.ChessGame.model.Entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@RequiredArgsConstructor
public class Square {
    private final Position position;
//  for square => position will not change

    @Setter
    private Piece piece;
// we can not use final for piece as the position of piece will keep on change based on move so
// @Setter helps to change the piece position

    public boolean isEmpty() {
        return piece == null;
    }
}
