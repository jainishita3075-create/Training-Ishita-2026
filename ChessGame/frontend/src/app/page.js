"use client";

import { useEffect, useRef, useState } from "react";
import styles from "./page.module.css";

const EMPTY_BOARD = Array.from({ length: 8 }, () => Array(8).fill(""));
const PIECES = {
  K: "♔", Q: "♕", R: "♖", B: "♗", N: "♘", P: "♙",
  k: "♚", q: "♛", r: "♜", b: "♝", n: "♞", p: "♟",
};
const FILES = "abcdefgh";
const TERMINAL_STATUSES = ["CHECKMATE", "STALEMATE", "DRAW", "RESIGNED"];

function authorizationHeader(credentials) {
  if (!credentials) return {};
  const bytes = new TextEncoder().encode(
    `${credentials.username}:${credentials.password}`
  );
  const binary = Array.from(bytes, (byte) => String.fromCharCode(byte)).join("");
  return { Authorization: `Basic ${btoa(binary)}` };
}

function encodeSocketField(value) {
  const bytes = new TextEncoder().encode(value || "");
  const binary = Array.from(bytes, (byte) => String.fromCharCode(byte)).join("");
  return btoa(binary);
}

async function requestApi(url, options, credentials) {
  const response = await fetch(url, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      ...authorizationHeader(credentials),
      ...options?.headers,
    },
  });
  const body = await response.json().catch(() => null);
  if (!response.ok) {
    throw new Error(body?.message || body?.error || `Request failed (${response.status})`);
  }
  return body;
}

function squareName(boardRow, column) {
  return `${FILES[column]}${8 - boardRow}`;
}

function ratingLabel(rating) {
  if (rating < 800) return "Learning";
  if (rating < 1200) return "Casual";
  if (rating < 1600) return "Club / intermediate";
  if (rating < 2000) return "Strong club";
  if (rating < 2200) return "Expert";
  if (rating < 2500) return "Master";
  return "Grandmaster";
}

export default function Home() {
  const [mode, setMode] = useState("home");
  const [accountMode, setAccountMode] = useState("register");
  const [name, setName] = useState("");
  const [password, setPassword] = useState("");
  const [rating, setRating] = useState("1200");
  const [credentials, setCredentials] = useState(null);
  const [playerName, setPlayerName] = useState("");
  const [ticketId, setTicketId] = useState("");
  const [game, setGame] = useState(null);
  const [selected, setSelected] = useState(null);
  const [promotion, setPromotion] = useState("q");
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  const requestInFlight = useRef(false);

  useEffect(() => {
    if (mode !== "queue" || !ticketId) return undefined;

    const checkMatch = async () => {
      try {
        const result = await requestApi(
          `/api/matchmaking/${ticketId}`, undefined, credentials
        );
        if (result.matched && result.gameId) {
          const state = await requestApi(
            `/api/games/${result.gameId}`, undefined, credentials
          );
          setGame(state);
          setMode("game");
          setError("");
        }
      } catch (e) {
        setError(e.message || "Could not check matchmaking status.");
      }
    };

    const timer = setInterval(checkMatch, 2000);
    return () => clearInterval(timer);
  }, [mode, ticketId, credentials]);

  useEffect(() => {
    if (mode !== "game" || !game?.gameId) return undefined;

    const refreshGame = async () => {
      if (requestInFlight.current || document.hidden) return;
      try {
        const latest = await requestApi(
          `/api/games/${game.gameId}`, undefined, credentials
        );
        setGame(latest);
      } catch (e) {
        setError(e.message || "Could not refresh the game.");
      }
    };

    const wsUrl = process.env.NEXT_PUBLIC_WS_URL
      || (window.location.hostname === "localhost"
        ? "ws://localhost:8080/ws/game"
        : "");
    let socket;

    if (wsUrl) {
      socket = new WebSocket(wsUrl);
      socket.onopen = () => {
        socket.send("AUTH|" + encodeSocketField(game.gameId)
          + "|" + encodeSocketField(credentials?.username)
          + "|" + encodeSocketField(credentials?.password));
      };
      socket.onmessage = (event) => {
        const message = JSON.parse(event.data);
        if (message.type === "AUTHENTICATED"
            || message.type === "GAME_UPDATED") {
          refreshGame();
        }
      };
    }

    // Keep a slower HTTP fallback in case the WebSocket disconnects.
    const timer = setInterval(refreshGame, 10000);
    return () => {
      clearInterval(timer);
      socket?.close();
    };
  }, [mode, game?.gameId, credentials]);

  async function joinMatchmaking(event) {
    event.preventDefault();
    const cleanName = name.trim();
    const numericRating = Number(rating);
    if (!cleanName) {
      setError("Enter your name to find a match.");
      return;
    }
    if (!password) {
      setError("Enter your password.");
      return;
    }
    if (accountMode === "register"
        && (!Number.isInteger(numericRating) || numericRating < 0)) {
      setError("Enter a valid non-negative Elo rating.");
      return;
    }

    setBusy(true);
    setError("");
    try {
      let accountName = cleanName.toLowerCase();
      if (accountMode === "register") {
        const account = await requestApi("/api/auth/register", {
          method: "POST",
          body: JSON.stringify({
            name: cleanName,
            password,
            eloRating: numericRating,
          }),
        });
        accountName = account.name;
      }

      const auth = { username: accountName, password };
      const result = await requestApi(
        "/api/matchmaking/join", { method: "POST" }, auth
      );
      setCredentials(auth);
      setPlayerName(accountName);
      setTicketId(result.ticketId || "");
      if (result.matched && result.gameId) {
        const state = await requestApi(
          `/api/games/${result.gameId}`, undefined, auth
        );
        setGame(state);
        setMode("game");
      } else if (result.ticketId) {
        setMode("queue");
      } else {
        throw new Error(result.message || "Could not join matchmaking.");
      }
    } catch (e) {
      setError(e.message || "Could not join matchmaking. Is the backend running?");
    } finally {
      setBusy(false);
    }
  }

  async function sendMove(from, to) {
    if (!game || requestInFlight.current) return;
    requestInFlight.current = true;
    setBusy(true);
    setError("");
    try {
      const movingPiece = game.board?.[from.row]?.[from.column] || "";
      const isPromotion = (movingPiece === "P" && to.row === 0)
        || (movingPiece === "p" && to.row === 7);
      const updated = await requestApi(`/api/games/${game.gameId}/moves`, {
        method: "POST",
        body: JSON.stringify({
          from: squareName(from.row, from.column),
          to: squareName(to.row, to.column),
          promotion: isPromotion ? promotion : null,
        }),
      }, credentials);
      setGame(updated);
      setSelected(null);
    } catch (e) {
      setError(e.message || "That move could not be made.");
    } finally {
      requestInFlight.current = false;
      setBusy(false);
    }
  }

  async function resignGame() {
    if (!game || requestInFlight.current) return;
    requestInFlight.current = true;
    setBusy(true);
    setError("");
    try {
      const updated = await requestApi(`/api/games/${game.gameId}/resign`, {
        method: "POST",
      }, credentials);
      setGame(updated);
      setSelected(null);
    } catch (e) {
      setError(e.message || "Could not resign the game.");
    } finally {
      requestInFlight.current = false;
      setBusy(false);
    }
  }

  async function undoLastMove() {
    if (!game || requestInFlight.current) return;
    requestInFlight.current = true;
    setBusy(true);
    setError("");
    try {
      const updated = await requestApi(`/api/games/${game.gameId}/undo`, {
        method: "POST",
      }, credentials);
      setGame(updated);
      setSelected(null);
    } catch (e) {
      setError(e.message || "Could not undo the last move.");
    } finally {
      requestInFlight.current = false;
      setBusy(false);
    }
  }

  function clickSquare(row, column) {
    if (!game || !myTurn || busy || gameOver) return;
    const piece = game.board?.[row]?.[column] || "";
    const isMine = myColor === "WHITE" ? piece !== "" && piece === piece.toUpperCase() : piece !== "" && piece === piece.toLowerCase();

    if (!selected) {
      if (isMine) setSelected({ row, column });
      return;
    }
    if (selected.row === row && selected.column === column) {
      setSelected(null);
      return;
    }
    if (isMine) {
      setSelected({ row, column });
      return;
    }
    sendMove(selected, { row, column });
  }

  const whitePlayer = game?.whitePlayer || "White";
  const blackPlayer = game?.blackPlayer || "Black";
  const myColor = game
    ? whitePlayer.toLowerCase() === playerName.toLowerCase() ? "WHITE" : "BLACK"
    : null;
  const myTurn = Boolean(game && game.currentTurn === myColor);
  const gameOver = Boolean(game && TERMINAL_STATUSES.includes(game.status));
  const statusText = gameOver
    ? game.status.replaceAll("_", " ").toLowerCase()
    : game?.status === "CHECK"
      ? `${game.currentTurn.toLowerCase()} is in check`
      : `${game?.currentTurn?.toLowerCase() || "white"} to move`;

  return (
    <main className={styles.page}>
      <header className={styles.topbar}>
        <a className={styles.brand} href="#home" onClick={(event) => event.preventDefault()}>
          <span className={styles.brandMark}>♞</span> CHECKMATE
        </a>
        <div className={styles.topbarRight}>
          <span className={styles.onlineDot} /> Two-player online chess
        </div>
      </header>

      {mode === "home" && (
        <section className={styles.landing}>
          <div className={styles.heroCopy}>
            <p className={styles.eyebrow}>YOUR NEXT MOVE STARTS HERE</p>
            <h1>Think ahead.<br /><em>Play together.</em></h1>
            <p className={styles.heroText}>A quiet board, a worthy opponent, and all the time in the world to make your move.</p>
            <div className={styles.featureLine}><span>♙</span> Standard chess rules <span className={styles.featureDivider}>·</span> Elo-based matchmaking</div>
          </div>
          <form className={styles.joinCard} onSubmit={joinMatchmaking}>
            <div className={styles.cardIcon}>♟</div>
            <h2>{accountMode === "register" ? "Create your account" : "Welcome back"}</h2>
            <p className={styles.cardSub}>Sign in securely, then find a player near your Elo level.</p>
            <label htmlFor="player-name">Username</label>
            <input id="player-name" value={name} onChange={(event) => setName(event.target.value)} placeholder="3–20 letters or numbers" maxLength={20} autoComplete="username" />
            <label htmlFor="player-password">Password</label>
            <input id="player-password" type="password" value={password} onChange={(event) => setPassword(event.target.value)} minLength={8} autoComplete={accountMode === "register" ? "new-password" : "current-password"} />
            {accountMode === "register" && <>
              <div className={styles.labelRow}>
                <label htmlFor="elo-rating">Starting Elo</label>
                <span>{ratingLabel(Number(rating) || 0)}</span>
              </div>
              <input id="elo-rating" type="number" min="0" max="4000" step="1" value={rating} onChange={(event) => setRating(event.target.value)} />
            </>}
            {error && <p className={styles.error} role="alert">{error}</p>}
            <button className={styles.primaryButton} type="submit" disabled={busy}>{busy ? "Please wait…" : accountMode === "register" ? "Create account & find match" : "Sign in & find match"}<span>→</span></button>
            <button className={styles.textButton} type="button" onClick={() => { setAccountMode(accountMode === "register" ? "login" : "register"); setError(""); }}>
              {accountMode === "register" ? "Already registered? Sign in" : "New here? Create an account"}
            </button>
          </form>
        </section>
      )}

      {mode === "queue" && (
        <section className={styles.queueCard}>
          <div className={styles.spinner} />
          <p className={styles.eyebrow}>MATCHMAKING</p>
          <h1>Looking for your opponent</h1>
          <p>Finding a player in your Elo group. This page will update when they join.</p>
          <div className={styles.queuePlayer}><span className={styles.avatar}>{playerName.slice(0, 1).toUpperCase()}</span><div><strong>{playerName}</strong><small>Using your account Elo for pairing</small></div><span className={styles.waitingBadge}>WAITING</span></div>
          {error && <p className={styles.error} role="alert">{error}</p>}
          <p className={styles.queueNote}>Returning to the form stops this screen from checking; your server-side ticket may still match because the backend has no cancel endpoint yet.</p>
          <button className={styles.textButton} onClick={() => { setMode("home"); setTicketId(""); setError(""); }}>Return to form</button>
        </section>
      )}

      {mode === "game" && game && (
        <section className={styles.gameLayout}>
          <div className={styles.gameMain}>
            <div className={styles.gameHeading}>
              <div><p className={styles.eyebrow}>MATCH IN PROGRESS</p><h1>The board is yours.</h1></div>
              <span className={`${styles.turnBadge} ${gameOver ? styles.endedBadge : ""}`}><span />{statusText}</span>
            </div>
            <div className={styles.playerStrip}>
              <PlayerCard name={blackPlayer} elo={game.blackElo} color="black" active={game.currentTurn === "BLACK" && !gameOver} />
              <div className={styles.vs}>VS</div>
              <PlayerCard name={whitePlayer} elo={game.whiteElo} color="white" active={game.currentTurn === "WHITE" && !gameOver} />
            </div>
            <div className={styles.boardFrame}>
              <div className={styles.rankLabels}>{[8, 7, 6, 5, 4, 3, 2, 1].map((rank) => <span key={rank}>{rank}</span>)}</div>
              <div className={styles.board} role="grid" aria-label="Chess board">
                {(game.board || EMPTY_BOARD).map((row, rowIndex) => row.map((piece, columnIndex) => {
                  const light = (rowIndex + columnIndex) % 2 === 1;
                  const isSelected = selected?.row === rowIndex && selected?.column === columnIndex;
                  return <button
                    className={`${styles.square} ${light ? styles.lightSquare : styles.darkSquare} ${isSelected ? styles.selectedSquare : ""}`}
                    key={`${rowIndex}-${columnIndex}`}
                    onClick={() => clickSquare(rowIndex, columnIndex)}
                    disabled={!myTurn || busy || gameOver}
                    aria-label={`${squareName(rowIndex, columnIndex)} ${piece ? `${piece === piece.toUpperCase() ? "white" : "black"} ${piece.toUpperCase()}` : "empty"}`}
                    role="gridcell"
                  >
                    {piece && <span className={`${styles.piece} ${piece === piece.toUpperCase() ? styles.whitePiece : styles.blackPiece}`}>{PIECES[piece] || piece}</span>}
                    {columnIndex === 0 && <span className={`${styles.coordinate} ${light ? styles.darkCoordinate : styles.lightCoordinate}`}>{8 - rowIndex}</span>}
                    {rowIndex === 7 && <span className={`${styles.fileCoordinate} ${light ? styles.darkCoordinate : styles.lightCoordinate}`}>{FILES[columnIndex]}</span>}
                  </button>;
                }))}
              </div>
            </div>
            <div className={styles.boardFooter}>
              <span>{myTurn ? "Your turn — select one of your pieces." : gameOver ? "This game has ended." : "Waiting for the other player to move."}</span>
              <label className={styles.promotionControl}>Promotion
                <select value={promotion} onChange={(event) => setPromotion(event.target.value)}>
                  <option value="q">Queen</option><option value="r">Rook</option><option value="b">Bishop</option><option value="n">Knight</option>
                </select>
              </label>
            </div>
            {error && <p className={styles.error} role="alert">{error}</p>}
          </div>

          <aside className={styles.sidebar}>
            <div className={styles.sidePanel}>
              <div className={styles.panelHeading}><h2>Move history</h2><span>{game.moveHistory?.length || 0} plies</span></div>
              {game.moveHistory?.length ? (
                <div className={styles.moveList}>
                  {Array.from({ length: Math.ceil(game.moveHistory.length / 2) }, (_, index) => (
                    <div className={styles.moveRow} key={index}><span className={styles.moveNumber}>{index + 1}.</span><span>{game.moveHistory[index * 2]}</span><span>{game.moveHistory[index * 2 + 1] || "—"}</span></div>
                  ))}
                </div>
              ) : <p className={styles.emptyHistory}>Your first move is waiting.</p>}
            </div>
            <div className={styles.sidePanel}>
              <h2>Game details</h2>
              <div className={styles.detailRow}><span>Game ID</span><strong title={game.gameId}>{game.gameId.slice(0, 8)}…</strong></div>
              <div className={styles.detailRow}><span>You are</span><strong>{myColor === "WHITE" ? "White" : "Black"}</strong></div>
              <div className={styles.detailRow}><span>Game status</span><strong>{game.status.replaceAll("_", " ")}</strong></div>
              {!gameOver && <>
                <button className={styles.resignButton} onClick={undoLastMove} disabled={busy || !game.moveHistory?.length}>Undo last move</button>
                <button className={styles.resignButton} onClick={resignGame} disabled={busy}>Resign game</button>
              </>}
            </div>
            <button className={styles.leaveButton} onClick={() => { setMode("home"); setGame(null); setTicketId(""); setSelected(null); setError(""); }}>Back to home</button>
          </aside>
        </section>
      )}
      <footer className={styles.footer}><span>CHECKMATE</span><span>One move at a time.</span></footer>
    </main>
  );
}

function PlayerCard({ name, elo, color, active }) {
  return (
    <div className={`${styles.playerCard} ${active ? styles.activePlayer : ""}`}>
      <span className={`${styles.playerStone} ${color === "black" ? styles.blackStone : styles.whiteStone}`} />
      <div className={styles.playerInfo}><strong>{name}</strong><small>{elo} Elo</small></div>
      {active && <span className={styles.activeLabel}>TO MOVE</span>}
    </div>
  );
}
