package com.example.ElevatorSystem.controller;

import com.example.ElevatorSystem.dto.ElevatorRequest;
import com.example.ElevatorSystem.dto.MoveElevatorRequest;
import com.example.ElevatorSystem.model.Elevator;
import com.example.ElevatorSystem.service.ElevatorService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/elevators")
public class ElevatorController {

    private final ElevatorService elevatorService;

    public ElevatorController(ElevatorService elevatorService) {
        this.elevatorService = elevatorService;
    }

    @GetMapping
    public List<Elevator> getAllElevators() {
        return elevatorService.getElevators();
    }

    @GetMapping("/{id}")
    public Elevator getElevator(
            @PathVariable int id
    ) {
        return elevatorService.getElevator(id);
    }

    @PostMapping("/process")
    public Elevator processNextRequest() {
        return elevatorService.processNextRequest();
    }

    @PostMapping("/step")
    public List<Elevator> stepAllElevators() {
        return elevatorService.stepAllElevators();
    }

    @GetMapping("/queue/size")
    public int getPendingRequestCount() {
        return elevatorService.getPendingRequestCount();
    }

    @PostMapping("/request")
    public String requestElevator(
            @Valid @RequestBody ElevatorRequest request
    ) {
        elevatorService.addRequest(request);

        return "Elevator request added to queue";
    }

    @PostMapping("/{id}/move")
    public Elevator moveElevator(
            @PathVariable int id,
            @Valid @RequestBody MoveElevatorRequest request
    ) {
        return elevatorService.goToFloor(
                id,
                request.getFloor()
        );
    }

    @PutMapping("/{id}/weight")
    public Elevator changeWeight(
            @PathVariable int id,
            @RequestParam double weight
    ) {
        return elevatorService.changeWeight(
                id,
                weight
        );
    }

    @PostMapping("/{id}/weight/remove")
    public Elevator removePassengerWeight(
            @PathVariable int id,
            @RequestParam @Positive double weight
    ) {
        return elevatorService.removePassengerWeight(id, weight);
    }

}
