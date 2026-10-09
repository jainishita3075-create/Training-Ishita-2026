package com.example.ElevatorSystem.service;

import com.example.ElevatorSystem.DefaultElevatorFactory;
import com.example.ElevatorSystem.dto.ElevatorRequest;
import com.example.ElevatorSystem.exception.ElevatorOverloadedException;
import com.example.ElevatorSystem.exception.InvalidFloorException;
import com.example.ElevatorSystem.model.Direction;
import com.example.ElevatorSystem.model.Elevator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ElevatorServiceTest {
    private ElevatorService elevatorService;

    @BeforeEach
    void setup() {
        elevatorService = new ElevatorService(new DefaultElevatorFactory());
    }

    @Test
    void invalidFloorThrowsException() {
        ElevatorRequest request = makeRequest(15, 17, Direction.UP, 50);
        assertThrows(InvalidFloorException.class,
                () -> elevatorService.addRequest(request));
    }

    @Test
    void requestIsAssignedToAnElevator() {
        elevatorService.addRequest(makeRequest(4, 7, Direction.UP, 50));
        Elevator elevator = elevatorService.processNextRequest();
        assertNotNull(elevator);
        assertEquals(4, elevator.getTargetFloor());
    }

    @Test
    void elevatorMovesToRequestedFloor() {
        Elevator elevator = elevatorService.goToFloor(2, 7);
        assertEquals(1, elevator.getCurrentFloor());
        for (int i = 0; i < 6; i++) {
            elevatorService.stepAllElevators();
        }
        assertEquals(7, elevator.getCurrentFloor());
        assertNull(elevator.getTargetFloor());
        assertEquals(Direction.IDLE, elevator.getDirection());
    }

    @Test
    void weightCanBeChanged() {
        Elevator elevator = elevatorService.changeWeight(3, 250);
        assertEquals(3, elevator.getId());
        assertEquals(250, elevator.getCurrentWeight());
    }

    @Test
    void weightAboveCapacityThrowsException() {
        assertThrows(ElevatorOverloadedException.class,
                () -> elevatorService.changeWeight(1, 600));
    }

    private ElevatorRequest makeRequest(
            int floor,
            int destinationFloor,
            Direction direction,
            double passengerWeight) {
        ElevatorRequest request = new ElevatorRequest();
        request.setFloor(floor);
        request.setDestinationFloor(destinationFloor);
        request.setDirection(direction);
        request.setPassengerWeight(passengerWeight);
        return request;
    }
}