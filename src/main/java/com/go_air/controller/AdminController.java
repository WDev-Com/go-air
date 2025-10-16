package com.go_air.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.go_air.service.AdminService;
import com.go_air.entity.Flights;
import java.util.List;

@RestController
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private AdminService adminService;

    // Create flight
    @PostMapping("/flights")
    public ResponseEntity<Flights> createFlight(@RequestBody Flights flight) {
        Flights createdFlight = adminService.createFlight(flight);
        return ResponseEntity.status(201).body(createdFlight); // 201 Created
    }

    // Get all flights
    @GetMapping("/flights")
    public ResponseEntity<List<Flights>> getAllFlights() {
        List<Flights> flights = adminService.getAllFlights();
        return ResponseEntity.ok(flights); // 200 OK
    }

    //  Get flight by flight number
    @GetMapping("/flights/{flightNumber}")
    public ResponseEntity<Flights> getFlightByFlightNumber(@PathVariable String flightNumber) {
        Flights flight = adminService.getFlightByFlightNumber(flightNumber);
        if (flight != null) {
            return ResponseEntity.ok(flight);
        } else {
            return ResponseEntity.notFound().build(); // 404 Not Found
        }
    }

    // Update flight by flight number
    @PutMapping("/flights/{flightNumber}")
    public ResponseEntity<Flights> updateFlightByFlightNumber(@PathVariable String flightNumber, @RequestBody Flights flight) {
        Flights updatedFlight = adminService.updateFlightByFlightNumber(flightNumber, flight);
        if (updatedFlight != null) {
            return ResponseEntity.ok(updatedFlight);
        } else {
            return ResponseEntity.notFound().build(); // 404 Not Found
        }
    }

    // Delete flight by flight number
    @DeleteMapping("/flights/{flightNumber}")
    public ResponseEntity<String> deleteFlightByFlightNumber(@PathVariable String flightNumber) {
        String result = adminService.deleteFlightByFlightNumber(flightNumber);
        return ResponseEntity.ok(result); // 200 OK with message
    }
}
