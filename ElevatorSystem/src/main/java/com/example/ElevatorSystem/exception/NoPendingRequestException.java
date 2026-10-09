package com.example.ElevatorSystem.exception;

public class NoPendingRequestException extends RuntimeException {

    public NoPendingRequestException(String message) {
        super(message);
    }
}
