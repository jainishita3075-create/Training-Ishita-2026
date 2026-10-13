package com.example.ChessGame.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RegisterRequest {
    private String name;
    private String password;
    private int eloRating = 1200;
}
