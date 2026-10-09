package com.example.ElevatorSystem;

import com.example.ElevatorSystem.model.Elevator;

public interface ElevatorFactory {

    Elevator createElevator(
        int id, String name, int startingFloor);
}
