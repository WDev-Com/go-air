package com.go_air.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.go_air.service.AdminService;
import com.go_air.entity.Flights;
import com.go_air.aop.ValidateFlightData;

import java.util.List;

@RestController
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private AdminService adminService;

    @PreAuthorize("hasAuthority('ADMIN')")
    // Create flight
    @PostMapping("/flights")
    @ValidateFlightData
    public ResponseEntity<Flights> createFlight(@RequestBody Flights flight) {
        Flights createdFlight = adminService.createFlight(flight);
        return ResponseEntity.status(201).body(createdFlight);
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    // Get all flights
    @GetMapping("/flights")
    public ResponseEntity<List<Flights>> getAllFlights() {
        return ResponseEntity.ok(adminService.getAllFlights());
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    // Get flight by flight number
    @GetMapping("/flights/{flightNumber}")
    public ResponseEntity<Flights> getFlightByFlightNumber(@PathVariable String flightNumber) {
        Flights flight = adminService.getFlightByFlightNumber(flightNumber);
        return (flight != null) ? ResponseEntity.ok(flight) : ResponseEntity.notFound().build();
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    // Update flight
    @PutMapping("/flights/{flightNumber}")
    @ValidateFlightData
    public ResponseEntity<Flights> updateFlightByFlightNumber(
            @PathVariable String flightNumber,
            @RequestBody Flights flight) {
        Flights updatedFlight = adminService.updateFlightByFlightNumber(flightNumber, flight);
        return (updatedFlight != null) ? ResponseEntity.ok(updatedFlight) : ResponseEntity.notFound().build();
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    // Delete flight
    @DeleteMapping("/flights/{flightNumber}")
    public ResponseEntity<String> deleteFlightByFlightNumber(@PathVariable String flightNumber) {
        String result = adminService.deleteFlightByFlightNumber(flightNumber);
        return ResponseEntity.ok(result);
    }
}
