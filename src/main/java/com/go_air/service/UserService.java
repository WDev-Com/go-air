package com.go_air.service;

import com.go_air.entity.Booking;
import com.go_air.entity.Flights;
import com.go_air.entity.Passenger;
import com.go_air.entity.User;
import com.go_air.enums.BookingStatus;
import com.go_air.enums.BookingType;
import com.go_air.enums.DepartureType;
import com.go_air.model.dtos.BookingResponseDTO;
import com.go_air.model.dtos.PassengerResponseDTO;
import com.go_air.model.dtos.PassengerTicketDTO;
import com.go_air.repo.BookingRepository;
import com.go_air.repo.FlightRepository;
import com.go_air.repo.PassengerRepository;
import com.go_air.repo.UserRepository;

import jakarta.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    @Autowired
    private BookingRepository bookingRepo;
    
    @Autowired
    private PassengerRepository passengerRepo;
    
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FlightRepository flightRepository;

    @Autowired
    private AdminService adminService; // fetch flight info

    
    public List<Flights> getFlightsByFilters(
            String airline,
            String sourceAirport,
            String destinationAirport,
            Integer stop,
            BookingType bookingType,
            DepartureType departureType,
            Double minPrice,
            Double maxPrice,
            Integer passengers
    ) {
        return flightRepository.findFlightsByFilters(
                airline, sourceAirport, destinationAirport, stop,
                bookingType, departureType, minPrice, maxPrice, passengers
        );
    }

    // Book a flight for a user with passengers
    @Transactional
    public Booking bookFlight(String userId, Booking bookingRequest) {
        // 1️ Check if user exists
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 2️ Fetch flight info from database
        Flights flight = flightRepository.findByFlightNumber(bookingRequest.getFlightNumber())
                .orElseThrow(() -> new RuntimeException("Flight not found"));

        LocalDate depDate = flight.getDepartureDate();
        LocalTime depTime = flight.getDepartureTime();
        LocalDate arrDate = flight.getArrivalDate();
        LocalTime arrTime = flight.getArrivalTime();

        List<Passenger> passengers = bookingRequest.getPassengers();

        for (int i = 0; i < passengers.size(); i++) {
            Passenger passenger = passengers.get(i);
            passenger.setUser(user);
            passenger = passengerRepo.save(passenger); // save new passenger

            // 3️ Check if passenger already booked the same flight
            boolean bookedSameFlight = bookingRepo
                    .existsByFlightNumberAndUser_UserIDAndPassengers_Name(
                            flight.getFlightNumber(),
                            userId,
                            passenger.getName()
                    );

            if (bookedSameFlight) {
                throw new RuntimeException("Passenger " + passenger.getName() +
                        " is already booked on flight " + flight.getFlightNumber());
            }

            // 4️ Check overlapping flights using passport number
            boolean hasConflict = bookingRepo.existsBookingConflictByPassport(
                    passenger.getPassportNumber(),
                    depDate,
                    depTime,
                    arrDate,
                    arrTime
            );

            if (hasConflict) {
                throw new RuntimeException("Passenger " + passenger.getName() +
                        " with passport " + passenger.getPassportNumber() +
                        " already has another flight overlapping between " +
                        depDate + " " + depTime + " and " +
                        arrDate + " " + arrTime);
            }

            passengers.set(i, passenger);
        }

        // 5️ Save booking
        bookingRequest.setPassengers(passengers);
        bookingRequest.setUser(user);
        bookingRequest.setBookingTime(LocalDateTime.now());
        bookingRequest.setStatus(BookingStatus.CONFIRMED);
        bookingRequest.setPassengerCount(passengers.size());

        // 6️ Use flight info to set times in booking
        bookingRequest.setDepartureDate(depDate);
        bookingRequest.setDepartureTime(depTime);
        bookingRequest.setArrivalDate(arrDate);
        bookingRequest.setArrivalTime(arrTime);

        Booking savedBooking = bookingRepo.save(bookingRequest);

        // 7️ Deduct seats from flight
        int remainingSeats = flight.getAvailableSeats() - savedBooking.getPassengerCount();
        if (remainingSeats < 0) {
            throw new RuntimeException("Not enough available seats on flight " + flight.getFlightNumber());
        }

        flight.setAvailableSeats(remainingSeats);
        flightRepository.save(flight);

        return savedBooking;
    }




    // Cancel booking
    public Booking cancelBooking(Long bookingId) {
        Booking booking = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        booking.setStatus(BookingStatus.CANCELLED);
        return bookingRepo.save(booking);
    }
    
    
    // Get all bookings for a user
    private List<PassengerResponseDTO> mapPassengers(List<Passenger> passengers) {
        if (passengers == null) return List.of();

        return passengers.stream()
                .<PassengerResponseDTO>map(p -> PassengerResponseDTO.builder()
                        .passengerId(p.getId())
                        .name(p.getName())
                        .gender(p.getGender())
                        .age(p.getAge())
                        .build())
                .collect(Collectors.toList());
    }

   // Get Tickets 
    public List<PassengerTicketDTO> generateTicketsByUser(String userId) {
    List<Booking> bookings = bookingRepo.findByUser_UserID(userId);
    List<PassengerTicketDTO> tickets = new ArrayList<>();

    for (Booking b : bookings) {
        Flights flight = adminService.getFlightByFlightNumber(b.getFlightNumber());

        for (Passenger p : b.getPassengers()) {
            tickets.add(PassengerTicketDTO.builder()
                    .ticketId(b.getId())
                    .flightNumber(b.getFlightNumber())
                    .tripType(b.getTripType())

                    // Use flight info if available, else booking info
                    .departureDate(flight != null && flight.getDepartureDate() != null
                            ? flight.getDepartureDate()
                            : b.getDepartureDate())
                    .departureTime(flight != null && flight.getDepartureTime() != null
                            ? flight.getDepartureTime()
                            : b.getDepartureTime())
                    .arrivalDate(flight != null && flight.getArrivalDate() != null
                            ? flight.getArrivalDate()
                            : b.getArrivalDate())
                    .arrivalTime(flight != null && flight.getArrivalTime() != null
                            ? flight.getArrivalTime()
                            : b.getArrivalTime())

                    // Flight details
                    .sourceAirport(flight != null ? flight.getSourceAirport() : null)
                    .destinationAirport(flight != null ? flight.getDestinationAirport() : null)
                    .stop(flight != null ? flight.getStop() : null)
                    .destinationStop(flight != null ? flight.getDestinationStop() : null)
                    .bookingType(flight != null ? flight.getBookingType() : null)
                    .departureType(flight != null ? flight.getDepartureType() : null)
                    .durationMinutes(flight != null ? flight.getDurationMinutes() : null)

                    // Booking info
                    .totalAmount(b.getPassengerCount() > 0 ? b.getTotalAmount() / b.getPassengerCount() : b.getTotalAmount())
                    .status(b.getStatus())

                    // Passenger info
                    .passengerName(p.getName())
                    .gender(p.getGender() != null ? p.getGender().toString() : null)
                    .age(p.getAge())
                    .build());
        }
    }

    return tickets;
}

    //By Booking ID Update this function TO_DO
    public List<BookingResponseDTO> getBookingsByUser(String userId) {
        return bookingRepo.findByUser_UserID(userId)
                .stream()
                .map((Booking b) -> BookingResponseDTO.builder()
                        .id(b.getId())
                        .flightNumber(b.getFlightNumber())
                        .tripType(b.getTripType())
                        .departureDate(b.getDepartureDate())
                        .departureTime(b.getDepartureTime())
                        .arrivalDate(b.getArrivalDate())
                        .arrivalTime(b.getArrivalTime())
                        .bookingTime(b.getBookingTime())
                        .passengerCount(b.getPassengerCount())
                        .totalAmount(b.getTotalAmount())
                        .status(b.getStatus())
                        .passengers(mapPassengers(b.getPassengers()))
                        .build())
                .collect(Collectors.toList());
    }


    
    // Create user
    public User createUser(User user) {
        if (user.getUserID() == null || user.getUserID().isEmpty()) {
            user.setUserID(generateUserID());
        }
        return userRepository.save(user);
    }

    private String generateUserID() {
        return "USR" + String.format("%03d", (int)(Math.random() * 1000));
    }

    // Get all users
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // Get user by ID
    public User getUserById(String userId) {
        return userRepository.findByUserID(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));
    }

    // Update user
    public User updateUser(String userId, User userDetails) {
        User existingUser = getUserById(userId);

        existingUser.setName(userDetails.getName());
        existingUser.setEmail(userDetails.getEmail());
        existingUser.setContact(userDetails.getContact());
        existingUser.setAddress(userDetails.getAddress());

        return userRepository.save(existingUser);
    }

    // Delete user
    public void deleteUser(String userId) {
        User existingUser = getUserById(userId);
        userRepository.delete(existingUser);
    }

}