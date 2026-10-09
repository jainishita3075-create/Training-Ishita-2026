package com.example.ElevatorSystem.model;

public class Elevator {
    private int id;
    private String name;
    private int currentFloor;
    private Integer targetFloor;
    private Direction direction;
    private double currentWeight;
    private double maxWeight;

    private Elevator(){}

    public Elevator(int id, String name, int currentFloor, Direction direction, double currentWeight, double maxWeight) {
        this.id = id;
        this.name = name;
        this.currentFloor = currentFloor;
        this.direction = direction;
        this.currentWeight = currentWeight;
        this.maxWeight = maxWeight;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getCurrentFloor() {
        return currentFloor;
    }

    public Integer getTargetFloor() {
        return targetFloor;
    }

    public void setTargetFloor(Integer targetFloor) {
        this.targetFloor = targetFloor;
    }

    public void setCurrentFloor(int currentFloor) {
        this.currentFloor = currentFloor;
    }

    public Direction getDirection() {
        return direction;
    }

    public void setDirection(Direction direction) {
        this.direction = direction;
    }

    public double getCurrentWeight() {
        return currentWeight;
    }

    public void setCurrentWeight(double currentWeight) {
        this.currentWeight = currentWeight;
    }

    public double getMaxWeight() {
        return maxWeight;
    }

    public void setMaxWeight(double maxWeight) {
        this.maxWeight = maxWeight;
    }

    public boolean isOverloaded() {
        return currentWeight > maxWeight;
    }
}
