package com.example.ElevatorSystem.exception;

public class ElevatorNotFoundException extends RuntimeException {

    public ElevatorNotFoundException(String message) {
        super(message);
    }
}