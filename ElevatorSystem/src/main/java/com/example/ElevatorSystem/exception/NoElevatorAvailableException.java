package com.example.ElevatorSystem.exception;

public class NoElevatorAvailableException extends RuntimeException {

    public NoElevatorAvailableException(String message) {
        super(message);
    }
}
