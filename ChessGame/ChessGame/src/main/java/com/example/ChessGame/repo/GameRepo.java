package com.example.ChessGame.repo;

import com.example.ChessGame.model.Entity.Game;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Repository
public class GameRepo {
//  concurrent mapping used:- saves and handles multiple threads accessing at same time
    private final ConcurrentMap<String, Game> games = new ConcurrentHashMap<>();

    public Game save(Game game) {
        games.put(game.getId(), game);
        return game;
    }

    public Optional<Game> findById(String id) {
        return Optional.ofNullable(games.get(id));
    }
}
