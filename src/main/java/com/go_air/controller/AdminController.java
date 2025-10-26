package com.go_air.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.go_air.service.AdminService;
import com.go_air.entity.Flights;
import com.go_air.entity.Seat;
import com.go_air.enums.SeatOperationStatus;
import com.go_air.aop.ValidateFlightData;

import java.util.List;

@RestController
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private AdminService adminService;
    
//    @Scheduled(fixedRate = 60000) // every 1 minute
//    public void updateFlightStatuses() {
//    	adminService.updateJourneyStatuses();
//    }
//    
    @PreAuthorize("hasAuthority('ADMIN')")
    @PostMapping("/generate-seats/{flightNo}")
    public ResponseEntity<String> generateSeats(@PathVariable String flightNo) {
        SeatOperationStatus status = adminService.generateSeatsForFlight(flightNo);

        switch (status) {
            case CREATED:
                return ResponseEntity.ok("Seats have been **created** successfully for flight: " + flightNo);
            case UPDATED:
                return ResponseEntity.ok("Seats were already created, status **updated** for flight: " + flightNo);
            case COMPLETED_NO_CHANGE:
                return ResponseEntity.badRequest().body("Cannot create or update seats because flight is **completed**: " + flightNo);
            default:
                return ResponseEntity.badRequest().body("Operation failed for flight: " + flightNo);
        }
    }


    @PreAuthorize("hasAuthority('ADMIN')")
    @GetMapping("/flight/{flightNumber}")
    public ResponseEntity<List<Seat>> getSeatsByFlightNo(@PathVariable String flightNumber) {
        List<Seat> seats = adminService.getSeatsByFlightNumber(flightNumber);
        return ResponseEntity.ok(seats);
    }

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

//    @PreAuthorize("hasAuthority('ADMIN')")
//    // Get flight by flight number
//    @GetMapping("/flights/{flightNumber}")
//    public ResponseEntity<Flights> getFlightByFlightNumber(@PathVariable String flightNumber) {
//        Flights flight = adminService.getFlightByFlightNumber(flightNumber);
//        return (flight != null) ? ResponseEntity.ok(flight) : ResponseEntity.notFound().build();
//    }

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
    
   /* Experimental Code */
    @PreAuthorize("hasAuthority('ADMIN')")
    @GetMapping("/flight/layout/{flightNumber}")
    public ResponseEntity<String> getSeatLayout(@PathVariable String flightNumber) {
        String layout = adminService.getSeatLayoutText(flightNumber);
        return ResponseEntity.ok(layout);
    }
}
