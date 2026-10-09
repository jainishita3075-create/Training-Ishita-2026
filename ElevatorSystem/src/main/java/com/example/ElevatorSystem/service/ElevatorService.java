package com.example.ElevatorSystem.service;

import com.example.ElevatorSystem.ElevatorFactory;
import com.example.ElevatorSystem.dto.ElevatorRequest;
import com.example.ElevatorSystem.exception.ElevatorNotFoundException;
import com.example.ElevatorSystem.exception.ElevatorOverloadedException;
import com.example.ElevatorSystem.exception.InvalidFloorException;
import com.example.ElevatorSystem.exception.InvalidWeightException;
import com.example.ElevatorSystem.exception.NoElevatorAvailableException;
import com.example.ElevatorSystem.exception.NoPendingRequestException;
import com.example.ElevatorSystem.model.Direction;
import com.example.ElevatorSystem.model.Elevator;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

@Service
public class ElevatorService {

    private static final int TOTAL_FLOORS = 10;
    private static final int TOTAL_ELEVATORS = 3;
    private final List<Elevator> elevators = new ArrayList<>();
    private final ElevatorFactory elevatorFactory;
    private final Queue<ElevatorRequest> requestQueue = new LinkedList<>();
    private final Map<Integer, Queue<PendingStop>> elevatorStops = new HashMap<>();
    private record PendingStop(int floor, double weightChange) {}

    public ElevatorService(ElevatorFactory elevatorFactory) {
        this.elevatorFactory = elevatorFactory;
        initializeElevators();
    }

    private void initializeElevators() {
        for (int i = 1; i <= TOTAL_ELEVATORS; i++) {
            Elevator elevator =
                    elevatorFactory.createElevator(
                            i,
                            "Elevator-" + i,
                            1
                    );
            elevators.add(elevator);
            elevatorStops.put(elevator.getId(), new LinkedList<>());
        }
    }

    public List<Elevator> getElevators() {
        return elevators;
    }

    public Elevator getElevator(int elevatorId) {
        return elevators.stream()
                .filter(elevator ->
                        elevator.getId() == elevatorId)
                .findFirst()
                .orElseThrow(() ->
                        new ElevatorNotFoundException(
                                "Elevator not found: "
                                        + elevatorId
                        ));
    }

    public int getPendingRequestCount() {
        return requestQueue.size();
    }

    public Elevator processNextRequest() {
        ElevatorRequest request = requestQueue.peek();
        if (request == null) {
            throw new NoPendingRequestException(
                    "No elevator request available"
            );
        }
        Elevator elevator = findBestElevator(request);
        requestQueue.poll();
        addStop(elevator, request.getFloor(), request.getPassengerWeight());
        addStop(elevator, request.getDestinationFloor(), -request.getPassengerWeight());
        return elevator;
    }

    private Elevator findBestElevator(ElevatorRequest request) {
        return elevators.stream()
                .filter(elevator -> elevator.getCurrentWeight()
                        + reservedWeight(elevator)
                        + request.getPassengerWeight() <= elevator.getMaxWeight())
                .min(Comparator
                        .comparingInt((Elevator elevator) ->
                                directionPriority(elevator, request.getDirection()))
                        .thenComparingInt(elevator -> Math.abs(
                                elevator.getCurrentFloor() - request.getFloor())))
                .orElseThrow(() -> new NoElevatorAvailableException(
                        "Request remains queued because no elevator currently has enough remaining weight capacity"));
    }

    private int directionPriority(
            Elevator elevator,
            Direction requestedDirection
    ) {
        if (elevator.getDirection()
                == requestedDirection) {
            return 0;
        }
        if (elevator.getDirection()
                == Direction.IDLE) {
            return 1;
        }
        return 2;
    }

    public Elevator goToFloor(
            int elevatorId,
            int destinationFloor
    ) {
        validateFloor(destinationFloor);
        Elevator elevator =
                getElevator(elevatorId);

        addStop(elevator, destinationFloor, 0);
        return elevator;
    }

    private void addStop(Elevator elevator, int floor, double passengerWeight) {
        elevatorStops.get(elevator.getId()).add(new PendingStop(floor, passengerWeight));
        if (elevator.getTargetFloor() == null) {
            activateNextStop(elevator);
        }
    }

    private double reservedWeight(Elevator elevator) {
        return elevatorStops.get(elevator.getId()).stream()
                .mapToDouble(PendingStop::weightChange)
                .filter(weightChange -> weightChange > 0)
                .sum();
    }

    private void activateNextStop(Elevator elevator) {
        Queue<PendingStop> stops = elevatorStops.get(elevator.getId());
        PendingStop nextStop = stops.peek();
        while (nextStop != null && nextStop.floor() == elevator.getCurrentFloor()) {
            stops.poll();
            elevator.setCurrentWeight(Math.max(0, elevator.getCurrentWeight()
                    + nextStop.weightChange()));
            nextStop = stops.peek();
        }
        if (nextStop == null) {
            elevator.setTargetFloor(null);
            elevator.setDirection(Direction.IDLE);
            return;
        }
        elevator.setTargetFloor(nextStop.floor());
        updateDirection(elevator);
    }

    private void updateDirection(Elevator elevator) {
        Integer targetFloor = elevator.getTargetFloor();
        if (targetFloor == null || targetFloor == elevator.getCurrentFloor()) {
            return;
        }
        elevator.setDirection(targetFloor > elevator.getCurrentFloor()
                ? Direction.UP : Direction.DOWN);
    }

    public List<Elevator> stepAllElevators() {
        for (Elevator elevator : elevators) {
            stepElevator(elevator);
        }
        return getElevators();
    }

    private void stepElevator(Elevator elevator) {
        Integer targetFloor = elevator.getTargetFloor();
        if (targetFloor == null) {
            return;
        }
        updateDirection(elevator);
        elevator.setCurrentFloor(elevator.getCurrentFloor()
                + (elevator.getDirection() == Direction.UP ? 1 : -1));
        if (elevator.getCurrentFloor() == targetFloor) {
            PendingStop reachedStop = elevatorStops.get(elevator.getId()).poll();
            elevator.setCurrentWeight(Math.max(0, elevator.getCurrentWeight()
                    + reachedStop.weightChange()));
            elevator.setTargetFloor(null);
            activateNextStop(elevator);
        }
    }

    public Elevator changeWeight(
            int elevatorId,
            double weight
    ) {
        Elevator elevator =
                getElevator(elevatorId);
        if (weight < 0) {
            throw new InvalidWeightException(
                    "Weight cannot be negative"
            );
        }
        if (weight + reservedWeight(elevator) > elevator.getMaxWeight()) {

            throw new ElevatorOverloadedException(
                    "Elevator maximum weight exceeded"
            );
        }
        elevator.setCurrentWeight(weight);
        return elevator;
    }

    public Elevator removePassengerWeight(int elevatorId, double passengerWeight) {
        Elevator elevator = getElevator(elevatorId);
        if (passengerWeight <= 0) {
            throw new InvalidWeightException("Weight to remove must be greater than zero");
        }
        if (passengerWeight > elevator.getCurrentWeight()) {
            throw new InvalidWeightException(
                    "Cannot remove more weight than the elevator currently carries"
            );
        }
        elevator.setCurrentWeight(elevator.getCurrentWeight() - passengerWeight);
        return elevator;
    }

    public void addRequest(ElevatorRequest request) {
        validateRequest(request);
        requestQueue.add(request);
    }

    private void validateRequest(
            ElevatorRequest request
    ) {
        if (request == null) {
            throw new IllegalArgumentException(
                    "Elevator request cannot be null"
            );
        }
        validateFloor(request.getFloor());
        validateFloor(request.getDestinationFloor());
        if (request.getFloor() == request.getDestinationFloor()) {
            throw new IllegalArgumentException(
                    "Destination floor must differ from pickup floor"
            );
        }
        if (request.getDirection() == null) {
            throw new IllegalArgumentException(
                    "Direction cannot be null"
            );
        }
        if (request.getDirection() == Direction.IDLE) {
            throw new IllegalArgumentException(
                    "A pickup request direction must be UP or DOWN"
            );
        }
        if ((request.getFloor() == TOTAL_FLOORS
                && request.getDirection() == Direction.UP)
                || (request.getFloor() == 1
                && request.getDirection() == Direction.DOWN)) {
            throw new IllegalArgumentException(
                    "The requested direction is not possible from this floor"
            );
        }
        if ((request.getDirection() == Direction.UP
                && request.getDestinationFloor() < request.getFloor())
                || (request.getDirection() == Direction.DOWN
                && request.getDestinationFloor() > request.getFloor())) {
            throw new IllegalArgumentException(
                    "Destination floor must be in the requested direction"
            );
        }
        if (request.getPassengerWeight() < 0) {
            throw new InvalidWeightException(
                    "Passenger weight cannot be negative"
            );
        }
        double maxCapacity = elevators.stream()
                .mapToDouble(Elevator::getMaxWeight)
                .max()
                .orElse(0);
        if (request.getPassengerWeight() > maxCapacity) {
            throw new ElevatorOverloadedException(
                    "Passenger weight exceeds every elevator's maximum capacity"
            );
        }
    }

    private void validateFloor(int floor) {
        if (floor < 1 || floor > TOTAL_FLOORS) {
            throw new InvalidFloorException(
                    "Floor must be between 1 and "
                            + TOTAL_FLOORS
            );
        }
    }
}