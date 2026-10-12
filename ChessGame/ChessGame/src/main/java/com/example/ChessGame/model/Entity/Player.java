package com.example.ChessGame.model.Entity;

import com.example.ChessGame.model.Enum.Color;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class Player {
    private Color color;
    private final String name;
    private int eloRating;

    public Player(String name, int eloRating){
        this.name = name;
        this.eloRating = eloRating;
    }

    public String getName() {
        return name;
    }

    public int getEloRating() {
        return eloRating;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public void updateEloRating(int change) {
        this.eloRating = Math.max(0, this.eloRating + change);
    }
}
