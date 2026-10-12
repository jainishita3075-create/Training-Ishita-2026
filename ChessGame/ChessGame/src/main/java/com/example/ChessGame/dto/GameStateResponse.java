package com.example.ChessGame.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class GameStateResponse {
    private String gameId;
    private String whitePlayer;
    private String blackPlayer;
    private int whiteElo;
    private int blackElo;
    private String currentTurn;
    private String status;
    private List<List<String>> board;
    private List<String> moveHistory;
}