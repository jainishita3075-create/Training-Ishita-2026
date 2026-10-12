package com.example.ChessGame.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MatchMakingResponse {
    private boolean matched;
    private String message;
    private String ticketId;
    private String gameId;
}
