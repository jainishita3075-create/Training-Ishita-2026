package com.example.ElevatorSystem;

import com.example.ElevatorSystem.model.Direction;
import com.example.ElevatorSystem.model.Elevator;
import org.springframework.stereotype.Component;

@Component
public class DefaultElevatorFactory implements ElevatorFactory{
    private static final double DEFAULT_MAX_WEIGHT = 300;

    @Override
    public Elevator createElevator(
            int id, String name, int startingFloor
    ){
        return new Elevator(
                id, name, startingFloor, Direction.IDLE, 0.0, DEFAULT_MAX_WEIGHT);
    }

}
