package com.example.ElevatorSystem.exception;

public class InvalidFloorException extends RuntimeException {

    public InvalidFloorException(String message) {
        super(message);
    }
}