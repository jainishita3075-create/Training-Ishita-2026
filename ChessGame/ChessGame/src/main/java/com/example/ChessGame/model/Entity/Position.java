package com.example.ChessGame.model.Entity;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
// helps to create .get() method which are req by other class
@RequiredArgsConstructor
// checks if 2 positions are same or not
// checks the parameters not the object can also be done by if-else
@EqualsAndHashCode
public class Position {
    private final int row;
    private final int column;

    public boolean isValid() {
        return row >= 0 && row < 8
                && column >= 0 && column < 8;

    }
}
