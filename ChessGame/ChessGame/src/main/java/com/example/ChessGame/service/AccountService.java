package com.example.ChessGame.service;

import com.example.ChessGame.model.Entity.Player;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class AccountService implements UserDetailsService {

    private final ConcurrentMap<String, Account> accounts =
            new ConcurrentHashMap<>();
    private final PasswordEncoder passwordEncoder;

    public AccountService(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    public String register(String name, String password, int eloRating) {
        if (name == null || !name.trim().matches("[A-Za-z0-9_.-]{3,20}")) {
            throw new IllegalArgumentException(
                    "Name must be 3-20 letters, numbers, dots, dashes, or underscores.");
        }
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException(
                    "Password must contain at least 8 characters.");
        }
        if (eloRating < 0) {
            throw new IllegalArgumentException("Elo rating cannot be negative.");
        }

        String normalizedName = normalize(name);
        Account account = new Account(
                normalizedName,
                passwordEncoder.encode(password),
                eloRating
        );

        if (accounts.putIfAbsent(normalizedName, account) != null) {
            throw new IllegalArgumentException("That username is already taken.");
        }

        return normalizedName;
    }

    public Player createPlayer(String name) {
        Account account = findAccount(name);
        return new Player(account.name, account.eloRating);
    }

    public void saveRating(String name, int rating) {
        Account account = accounts.get(normalize(name));
        if (account != null) {
            account.eloRating = rating;
        }
    }

    @Override
    public UserDetails loadUserByUsername(String name) {
        Account account = accounts.get(normalize(name));
        if (account == null) {
            throw new UsernameNotFoundException("Account not found.");
        }

        return User.withUsername(account.name)
                .password(account.passwordHash)
                .roles("PLAYER")
                .build();
    }

    private Account findAccount(String name) {
        Account account = accounts.get(normalize(name));
        if (account == null) {
            throw new IllegalArgumentException("Account not found.");
        }
        return account;
    }

    private String normalize(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name is required.");
        }
        return name.trim().toLowerCase(Locale.ROOT);
    }

    private static class Account {
        private final String name;
        private final String passwordHash;
        private volatile int eloRating;

        private Account(String name, String passwordHash, int eloRating) {
            this.name = name;
            this.passwordHash = passwordHash;
            this.eloRating = eloRating;
        }
    }
}
