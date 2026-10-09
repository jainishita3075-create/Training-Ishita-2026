package com.example.ElevatorSystem.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public class MoveElevatorRequest {
    @Min(1)
    @Max(10)
    private int floor;

    public int getFloor() {
        return floor;
    }

    public void setFloor(int floor) {
        this.floor = floor;
    }
}
