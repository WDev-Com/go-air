package com.go_air.controller;

import com.go_air.entity.Booking;
import com.go_air.entity.Flights;
import com.go_air.entity.User;
import com.go_air.enums.BookingType;
import com.go_air.enums.DepartureType;
import com.go_air.model.dtos.BookingResponseDTO;
import com.go_air.model.dtos.PassengerTicketDTO;
import com.go_air.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;
    
    // Search Flight
    @GetMapping("/flights")
    public ResponseEntity<List<Flights>> getFlightsByFilters(
            @RequestParam(required = false) String airline,
            @RequestParam(required = false) String sourceAirport,
            @RequestParam(required = false) String destinationAirport,
            @RequestParam(required = false) Integer stop,
            @RequestParam(required = false) BookingType bookingType,
            @RequestParam(required = false) DepartureType departureType,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) Integer passengers
    ) {
        List<Flights> flights = userService.getFlightsByFilters(
                airline, sourceAirport, destinationAirport, stop,
                bookingType, departureType, minPrice, maxPrice, passengers
        );
        return ResponseEntity.ok(flights);
    }

    // For Booking
    @PostMapping("/book/{userId}")
    public ResponseEntity<Map<String, Object>> bookFlight(
            @PathVariable String userId,
            @RequestBody Booking bookingRequest) {
        Map<String, Object> response = new HashMap<>();
        
            Booking booking = userService.bookFlight(userId, bookingRequest);

            response.put("message", "Flight booked successfully!");
            response.put("userId", booking.getUser().getUserID());
            response.put("bookingId", booking.getId());
            response.put("status", booking.getStatus());
            response.put("bookingTime", booking.getBookingTime());

            return ResponseEntity.status(201).body(response); // 201 Created
       
    }

    // Get Tickets
    @GetMapping("/tickets/{userId}")
    public ResponseEntity<List<PassengerTicketDTO>> getPassengerTickets(@PathVariable String userId) {
        List<PassengerTicketDTO> tickets = userService.generateTicketsByUser(userId);
        return ResponseEntity.ok(tickets);
    }
    
    // Get all bookings for a user
    @GetMapping("/bookings/{userId}")
    public ResponseEntity<List<BookingResponseDTO>> getUserBookings(@PathVariable String userId) {
        List<BookingResponseDTO> bookings = userService.getBookingsByUser(userId); // service now returns DTO
        return ResponseEntity.ok(bookings);
    }


    
    // Cancel a booking
    @PutMapping("/cancel/{bookingId}")
    public ResponseEntity<Map<String, String>> cancelBooking(@PathVariable Long bookingId) {
        Booking cancelledBooking = userService.cancelBooking(bookingId);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Booking cancelled successfully");
        response.put("bookingId", String.valueOf(cancelledBooking.getId()));
        response.put("status", cancelledBooking.getStatus().toString());

        return ResponseEntity.ok(response); // 200 OK
    }

   
    // Create user
    @PostMapping("/create")
    public ResponseEntity<User> createUser(@RequestBody User user) {
        User createdUser = userService.createUser(user);
        return ResponseEntity.status(201).body(createdUser);
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

    // Update user
    @PutMapping("/{userId}")
    public ResponseEntity<User> updateUser(@PathVariable String userId, @RequestBody User user) {
        User updatedUser = userService.updateUser(userId, user);
        if (updatedUser != null) {
            return ResponseEntity.ok(updatedUser);
        } else {
            return ResponseEntity.notFound().build();
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
