# Two Player Chess

A small Spring Boot starter for a standard two-player chess game. The browser UI is served by Spring Boot, while the server owns the game state and validates every move.

## Run

Requirements: Java 21 and Maven (or use the included Maven Wrapper).

From this directory:

```powershell
./mvnw.cmd spring-boot:run
```

Open [http://localhost:8080](http://localhost:8080), enter two distinct player names, and play from the same browser. The server randomly assigns the white and black sides; white moves first.

## Features

- Standard legal move validation, including check, checkmate, stalemate, castling, en passant, and promotion.
- Alternating turns and server-side player-turn checks.
- Move history, resignation, and draw detection supplied by Chesslib.
- Responsive board and move history for a local two-player session.

## API

All game state is currently held in memory and is lost when the application restarts. The browser identifies the active player by name; this is a learning/demo setup, not authenticated online multiplayer.

| Method | Path | Request | Result |
|---|---|---|---|
| `POST` | `/api/games` | `{"firstPlayer":"Alex","secondPlayer":"Sam"}` | New game, randomized colors, board FEN and legal moves |
| `GET` | `/api/games/{id}` | — | Current game state |
| `POST` | `/api/games/{id}/moves` | `{"player":"Alex","from":"e2","to":"e4","promotion":""}` | Applies a legal move and returns updated state |
| `POST` | `/api/games/{id}/resign` | `{"player":"Alex"}` | Ends the game by resignation |

Move requests use algebraic square coordinates (`a1` through `h8`). For promotion, set `promotion` to `q`, `r`, `b`, or `n`.

## Design notes

- `ChessGame` contains one authoritative chess board, participants, status and move log. A separate `Square` object is unnecessary because each square is already represented by its coordinate in the rules engine.
- `ChessGameService` manages in-memory game instances and applies game actions.
- `ChessGameController` exposes the REST API; `GameView` is the response model.
- `src/main/resources/static` contains the browser client.

To support real online play next, add authentication, persistent storage, and a live update channel such as WebSocket. Replace the demo player-name check with authenticated player identities.
