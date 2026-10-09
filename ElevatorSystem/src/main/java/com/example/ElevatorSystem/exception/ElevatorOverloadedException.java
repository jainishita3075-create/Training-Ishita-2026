package com.example.ElevatorSystem.exception;

public class ElevatorOverloadedException extends RuntimeException {

    public ElevatorOverloadedException(String message) {
        super(message);
    }
}
