package com.example.ElevatorSystem.dto;

import com.example.ElevatorSystem.model.Direction;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public class ElevatorRequest {

    @Min(1)
    @Max(10)
    private int floor;

    @Min(1)
    @Max(10)
    private int destinationFloor;

    @NotNull
    private Direction direction;

    @PositiveOrZero
    private double passengerWeight;

    public int getFloor() {
        return floor;
    }

    public void setFloor(int floor) {
        this.floor = floor;
    }

    public int getDestinationFloor() {
        return destinationFloor;
    }

    public void setDestinationFloor(int destinationFloor) {
        this.destinationFloor = destinationFloor;
    }

    public Direction getDirection() {
        return direction;
    }

    public void setDirection(Direction direction) {
        this.direction = direction;
    }

    public double getPassengerWeight() {
        return passengerWeight;
    }

    public void setPassengerWeight(double passengerWeight) {
        this.passengerWeight = passengerWeight;
    }

}
