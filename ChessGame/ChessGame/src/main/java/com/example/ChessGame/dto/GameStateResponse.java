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
    private int whiteCapturedPoints;
    private int blackCapturedPoints;
    private String currentTurn;
    private String status;
    private long whiteTimeMillis;
    private long blackTimeMillis;
    private long clockUpdatedAtMillis;
    private List<List<String>> board;
    private List<String> moveHistory;
}
