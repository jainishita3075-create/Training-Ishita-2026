package com.example.ChessGame.controller;

import com.example.ChessGame.dto.RegisterRequest;
import com.example.ChessGame.service.AccountService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AccountService accountService;

    public AuthController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(
            @RequestBody RegisterRequest request) {
        String name = accountService.register(
                request.getName(),
                request.getPassword(),
                request.getEloRating()
        );

        return ResponseEntity.ok(Map.of(
                "name", name,
                "message", "Account created. You can now find a match."
        ));
    }
}
