package com.go_air.controller;

import com.go_air.aop.ValidateFlightData;
import com.go_air.entity.Booking;
import com.go_air.entity.Flights;
import com.go_air.entity.User;
import com.go_air.enums.AircraftSize;
import com.go_air.enums.BookingType;
import com.go_air.enums.DepartureType;
import com.go_air.enums.SpecialFareType;
import com.go_air.enums.TripType;
import com.go_air.model.dtos.BookingResponseDTO;
import com.go_air.model.dtos.PassengerTicketDTO;
import com.go_air.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;


@RestController
@CrossOrigin("*")
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;
    
    @GetMapping("/flights/search")
    @ValidateFlightData
    public ResponseEntity<?> searchFlights(
            @RequestParam TripType tripType,
            @RequestParam(required = false) List<String> airlines,
            @RequestParam String sourceAirports,       // comma-separated
            @RequestParam String destinationAirports,  // comma-separated
            @RequestParam(required = false) String departureDates, // comma-separated
            @RequestParam(required = false) String returnDate,
            @RequestParam(required = false) Integer stop,
            @RequestParam(required = false) BookingType bookingType,
            @RequestParam(required = false) DepartureType departureType,
            @RequestParam(required = false) Integer minPrice,
            @RequestParam(required = false) Integer maxPrice,
            @RequestParam(required = false) Integer passengers,
            @RequestParam(required = false) AircraftSize aircraftSize,
            @RequestParam(required = false) SpecialFareType specialFareType //enum param
    ) {

        // Parse comma-separated airport and date values
        List<String> sources = Arrays.stream(sourceAirports.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        List<String> destinations = Arrays.stream(destinationAirports.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        List<LocalDate> dates = Arrays.stream(departureDates.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> LocalDate.parse(s, formatter))
                .toList();
        
        LocalDate retDate = null;
        if (returnDate != null && !returnDate.isEmpty()) {
            retDate = LocalDate.parse(returnDate, DateTimeFormatter.ISO_DATE);           
        }
      
     // Clean airline list
        List<String> validAirlines = (airlines != null)
                ? airlines.stream().map(String::trim).filter(s -> !s.isEmpty()).toList()
                : List.of();

        // Call service method
        Map<String, List<Flights>> flights = userService.searchFlightsByTripType(
                tripType,
                validAirlines,
                sources,
                destinations,
                dates,
                retDate,
                stop,
                bookingType,
                departureType,
                minPrice,
                maxPrice,
                passengers,
                aircraftSize,
                specialFareType
        );

        return ResponseEntity.ok(flights);
    }

    
    // Search Flight
    @GetMapping("/flights")
    @ValidateFlightData
    public ResponseEntity<List<Flights>> getFlightsByFilters(
    		@RequestParam(required = false) List<String> airlines,
            @RequestParam(required = false) String sourceAirport,
            @RequestParam(required = false) String destinationAirport,
            @RequestParam(required = false) String departureDateStr, 
            @RequestParam(required = false) Integer stop,
            @RequestParam(required = false) BookingType bookingType,
            @RequestParam(required = false) DepartureType departureType,
            @RequestParam(required = false) Integer minPrice,
            @RequestParam(required = false) Integer maxPrice,
            @RequestParam(required = false) Integer passengers,
            @RequestParam(required = false) AircraftSize aircraftSize
    ) {
        LocalDate departureDate = null;
        if (departureDateStr != null && !departureDateStr.trim().isEmpty()) {
            departureDate = LocalDate.parse(departureDateStr.trim());
        }
        
     // Clean airline list
        List<String> validAirlines = (airlines != null)
                ? airlines.stream().map(String::trim).filter(s -> !s.isEmpty()).toList()
                : List.of();

        
        List<Flights> flights = userService.getFlightsByFilters(
        		validAirlines, sourceAirport, destinationAirport, departureDate, stop,
                bookingType, departureType, minPrice, maxPrice, passengers,aircraftSize
        );
        return ResponseEntity.ok(flights);
    }


    // For Booking
    @PostMapping("/book/{userId}")
    @ValidateFlightData
    public ResponseEntity<Map<String, Object>> bookFlights(
            @PathVariable String userId,
            @RequestBody List<Booking> bookingRequests) {

        Map<String, Object> response = new HashMap<>();
        List<Long> bookingIds = new ArrayList<>();

        for (Booking bookingRequest : bookingRequests) {
            Booking booking = userService.bookFlight(userId, bookingRequest);
            bookingIds.add(booking.getId());
        }

        response.put("message", "Flights booked successfully!");
        response.put("userId", userId);
        response.put("bookingIds", bookingIds);
        response.put("status", "CONFIRMED");
        response.put("bookingTime", LocalDateTime.now());

        return ResponseEntity.status(201).body(response);
    }


    // Get Tickets
    @GetMapping("/tickets/{userId}")
    public ResponseEntity<List<PassengerTicketDTO>> getPassengerTickets(@PathVariable String userId) {
        List<PassengerTicketDTO> tickets = userService.generateTicketsByUser(userId);
        return ResponseEntity.ok(tickets);
    }
    
    // Get Ticket by userId and bookingId
    @GetMapping("/tickets/{userId}/{bookingId}")
    public ResponseEntity<List<PassengerTicketDTO>> getPassengerTicketsByBooking(
            @PathVariable String userId,
            @PathVariable Long bookingId) {

        List<PassengerTicketDTO> tickets = userService.generateTicketsByUserAndBooking(userId, bookingId);
        return ResponseEntity.ok(tickets);
    }

    
    // Get all bookings for a user
    @GetMapping("/bookings/{userId}")
    public ResponseEntity<List<BookingResponseDTO>> getUserBookings(@PathVariable String userId) {
        List<BookingResponseDTO> bookings = userService.getBookingsByUser(userId); // service now returns DTO
        return ResponseEntity.ok(bookings);
    }

    // Cancel a booking
    @PutMapping("/cancel/{bookingNo}")
    public ResponseEntity<Map<String, Object>> cancelBooking(@PathVariable String bookingNo) {
        Map<String, Object> response = userService.cancelBooking(bookingNo);
        return ResponseEntity.ok(response); // 200 OK
    }



    // Get all users
    @GetMapping("/get-all")
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    // Get user by ID
    @GetMapping("/{userId}")
    public ResponseEntity<User> getUserById(@PathVariable String userId) {
        User user = userService.getUserById(userId);
        if (user != null) {
            return ResponseEntity.ok(user);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    // Get user by username
    @GetMapping("/username/{username}")
    public ResponseEntity<?> getUserByUsername(@PathVariable String username) {
        try {
            User user = userService.findByUserName(username);
            if (user == null) {
                return ResponseEntity.status(404).body("User not found");
            }
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error fetching user: " + e.getMessage());
        }
    }

    // Delete user
    @DeleteMapping("/{userId}")
    public ResponseEntity<Map<String, String>> deleteUser(@PathVariable String userId) {
        userService.deleteUser(userId);
        Map<String, String> response = new HashMap<>();
        response.put("message", "User deleted successfully!");
        return ResponseEntity.ok(response);
    }
}
