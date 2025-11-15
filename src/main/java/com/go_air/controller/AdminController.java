package com.go_air.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.go_air.service.AdminService;
import com.go_air.entity.Flights;
import com.go_air.enums.AircraftSize;
import com.go_air.enums.BookingStatus;
import com.go_air.enums.BookingType;
import com.go_air.enums.DepartureType;
import com.go_air.enums.JourneyStatus;
import com.go_air.enums.SeatOperationStatus;
import com.go_air.enums.SpecialFareType;
import com.go_air.enums.TripType;
import com.go_air.model.dtos.BookingResponseDTO;
import com.go_air.aop.ValidateFlightData;
import java.util.List;
import java.util.Map;

//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

@RestController
@CrossOrigin("*")
@RequestMapping("/admin")
public class AdminController {

	//private static final Logger log = LoggerFactory.getLogger(AdminService.class);

    @Autowired
    private AdminService adminService;
    
    @Scheduled(fixedRate = 60000) // every 1 minute
    public void updateFlightStatuses() {
    	adminService.updateJourneyStatuses();
    }
    
    
    @PreAuthorize("hasAuthority('ADMIN')")
    @GetMapping("/searchByPaginationAndFilters")
    public Map<String, Object> searchFlightsByPaginationAndFilters(
            @RequestParam(required = false) List<String> airlines,
            @RequestParam(required = false) String sourceAirport,
            @RequestParam(required = false) String destinationAirport,
            @RequestParam(required = false) String departureDate,
            @RequestParam(required = false) Integer stop,
            @RequestParam(required = false) BookingType bookingType,
            @RequestParam(required = false) DepartureType departureType,
            @RequestParam(required = false) Integer minPrice,
            @RequestParam(required = false) Integer maxPrice,
            @RequestParam(required = false) AircraftSize aircraftSize,
            @RequestParam(required = false) SpecialFareType specialFareType,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "5") Integer limit
    ) {
        if (stop != null && stop == 0) stop = null;
        airlines = cleanList(airlines);


        return adminService.searchFlightsWithPagination(
                airlines,
                sourceAirport,
                destinationAirport,
                departureDate,
                stop,
                bookingType,
                departureType,
                minPrice,
                maxPrice,
                aircraftSize,
                specialFareType,
                page,
                limit
        );
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    // Get Booking by filters
    @GetMapping("/bookings/search")
    public ResponseEntity<List<BookingResponseDTO>> getAllBookingsByFilters(
            @RequestParam(required = false) String bookingNo,
            @RequestParam(required = false) String flightNumber,
            @RequestParam(required = false) TripType tripType,
            @RequestParam(required = false) String userName,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(required = false) SpecialFareType specialFareType,
            @RequestParam(required = false) JourneyStatus journeyStatus
    ) {
        List<BookingResponseDTO> bookings = adminService.getBookingsByFilters(
                bookingNo,
                flightNumber,
                tripType,
                userName,
                userId,
                status,
                specialFareType,
                journeyStatus
        );

        return ResponseEntity.ok(bookings);
    }




    // ---------- Helper methods ----------
  
    // 🔹 Helper to remove empty strings or "Select" options
    private List<String> cleanList(List<String> list) {
        if (list == null) return null;
        List<String> cleaned = list.stream()
                .filter(s -> s != null && !s.trim().isEmpty() && !s.startsWith("Select"))
                .toList();
        return cleaned.isEmpty() ? null : cleaned;
    }


  
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
   // Get flight by flight number
   @GetMapping("/flights/{flightNumber}")
   public ResponseEntity<Flights> getFlightByFlightNo(@PathVariable String flightNumber) {
      Flights flight = adminService.getFlightByFlightNumber(flightNumber);
      return (flight != null) ? ResponseEntity.ok(flight) : ResponseEntity.notFound().build();
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
