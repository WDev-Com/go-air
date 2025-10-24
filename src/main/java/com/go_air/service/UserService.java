package com.go_air.service;

import com.go_air.entity.Booking;
import com.go_air.entity.Flights;
import com.go_air.entity.Passenger;
import com.go_air.entity.User;
import com.go_air.enums.BookingStatus;
import com.go_air.enums.BookingType;
import com.go_air.enums.DepartureType;
import com.go_air.enums.SpecialFareType;
import com.go_air.enums.TripType;
import com.go_air.model.dtos.BookingResponseDTO;
import com.go_air.model.dtos.PassengerResponseDTO;
import com.go_air.model.dtos.PassengerTicketDTO;
import com.go_air.repo.BookingRepository;
import com.go_air.repo.FlightRepository;
import com.go_air.repo.PassengerRepository;
import com.go_air.repo.UserRepository;



import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
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

    @Autowired
    private PasswordEncoder passwordEncoder;
    
    public List<Flights> getFlightsByDepartureDate(LocalDate departureDate) {
        return flightRepository.findFlightsByDepartureDate(departureDate);
    }
    
    
    public Map<String, List<Flights>> searchFlightsByTripType(
            TripType tripType,
            String airline,
            List<String> sourceAirports,
            List<String> destinationAirports,
            List<LocalDate> departureDates, // <-- accepts as String list
            Integer stop,
            BookingType bookingType,
            DepartureType departureType,
            Integer minPrice,
            Integer maxPrice,
            Integer passengers,
            SpecialFareType specialFareType
    ) {
        Map<String, List<Flights>> result = new LinkedHashMap<>();

        if (sourceAirports == null || destinationAirports == null
                || sourceAirports.isEmpty() || destinationAirports.isEmpty()) {
            throw new IllegalArgumentException("Source and destination airports must not be empty for the selected trip type.");
        }

        final String trimmedAirline = (airline != null && !airline.trim().isEmpty()) ? airline.trim() : null;

        // Clean source & destination lists
        List<String> validSources = sourceAirports.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        List<String> validDestinations = destinationAirports.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

 
        // Adjust price based on special fare type
        final Integer adjustedMaxPrice;
        if (specialFareType != null) {
            specialFareType.validatePassengers(passengers);
            adjustedMaxPrice = specialFareType.applyDiscount(maxPrice);
        } else {
            adjustedMaxPrice = maxPrice;
        }

        // Record for clean parameter passing
        record FlightParams(String src, String dest, LocalDate date) {}

        // Fetch + Discount function
        Function<FlightParams, List<Flights>> fetchAndDiscount = params ->
                applyPostFareDiscount(
                        getFlightsByFilters(
                                trimmedAirline, params.src(), params.dest(), params.date(),
                                stop, bookingType, departureType,
                                minPrice, adjustedMaxPrice, passengers
                        ),
                        specialFareType,
                        passengers
                );

        // Perform search based on trip type
        switch (tripType) {
            case ONE_WAY -> {
                String key = validSources.get(0) + "_" + validDestinations.get(0);
                result.put(key, fetchAndDiscount.apply(new FlightParams(
                        validSources.get(0),
                        validDestinations.get(0),
                        departureDates.get(0)
                )));
            }

            case ROUND_TRIP -> {
                String forwardKey = validSources.get(0) + "_" + validDestinations.get(0);
                String returnKey = validDestinations.get(0) + "_" + validSources.get(0);

                result.put(forwardKey, fetchAndDiscount.apply(new FlightParams(
                        validSources.get(0),
                        validDestinations.get(0),
                        departureDates.get(0)
                )));

             // Return flight should be on the next day
                LocalDate returnDate;
                if (departureDates.size() > 1) {
                    // If user provided both dates, use the second one
                    returnDate = departureDates.get(1);
                } else {
                    // Otherwise, automatically set as next day
                    returnDate = departureDates.get(0).plusDays(1);
                }

                result.put(returnKey, fetchAndDiscount.apply(new FlightParams(
                        validDestinations.get(0),
                        validSources.get(0),
                        returnDate
                )));
            }

            case MULTI_CITY -> {
                int trips = Math.min(
                        Math.min(validSources.size(), validDestinations.size()),
                        departureDates.size()
                );

                for (int i = 0; i < trips; i++) {
                    String src = validSources.get(i);
                    String dest = validDestinations.get(i);
                    LocalDate date = departureDates.get(i);

                    if (src.isEmpty() || dest.isEmpty()) continue;

                    String key = src + "_" + dest;
                    result.put(key, fetchAndDiscount.apply(new FlightParams(src, dest, date)));
                }
            }

            default -> throw new IllegalArgumentException("Unsupported TripType: " + tripType);
        }
        return result;
    }

    private List<Flights> applyPostFareDiscount(List<Flights> flights, SpecialFareType fareType, Integer passengers) {
        if (fareType == null) return flights;

        fareType.validatePassengers(passengers);

        for (Flights f : flights) {
        	Integer discountedPrice = fareType.applyDiscount(f.getPrice());
            f.setPrice((int) (Math.round(discountedPrice * 100.0) / 100.0)); // round to 2 decimals
        }
        return flights;
    }

    public List<Flights> getFlightsByFilters(
        String airline,
        String sourceAirport,
        String destinationAirport,
        LocalDate departureDate,
        Integer stop,
        BookingType bookingType,
        DepartureType departureType,
        Integer minPrice,
        Integer maxPrice,
        Integer passengers
) {
    airline = (airline != null && !airline.trim().isEmpty()) ? airline.trim() : null;
    sourceAirport = (sourceAirport != null && !sourceAirport.trim().isEmpty()) ? sourceAirport.trim() : null;
    destinationAirport = (destinationAirport != null && !destinationAirport.trim().isEmpty()) ? destinationAirport.trim() : null;

    String bookingTypeStr = (bookingType != null) ? bookingType.name() : null;
    String departureTypeStr = (departureType != null) ? departureType.name() : null;

    
    
    return flightRepository.findFlightsByFilters(
            airline,
            sourceAirport,
            destinationAirport,
            departureDate,
            stop,
            bookingTypeStr,
            departureTypeStr,
            minPrice,
            maxPrice,
            passengers
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
                throw new RuntimeException("## Passenger " + passenger.getName() +
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

    // Create user with only username and password
    public User createUser(User user) {
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new RuntimeException("Username already exists");
        }
        
        User newUser = new User();
        newUser.setUserID(generateUserID());
        newUser.setUsername(user.getUsername());
        newUser.setPassword(passwordEncoder.encode(user.getPassword()));
        newUser.setName(null);
        newUser.setAddress(null);
        newUser.setContact(null);
        newUser.setEmail(null);
        newUser.setRole("USER"); // default role
        return userRepository.save(newUser);
    }

    // Check if username exists
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    // Generate User ID
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
    @Transactional
    public User updateUserByUsername(String username, User userDetails) {
        User existingUser = userRepository.findByUsername(username);
        if (existingUser == null) {
            throw new IllegalArgumentException("User not found with username: " + username);
        }
        // Check if contact or email already exists for another user
        User userByContact = userRepository.findByContact(userDetails.getContact());
        if (userByContact != null && !userByContact.getUsername().equals(username)) {
            throw new RuntimeException("User with this contact already exists!");
        }

        User userByEmail = userRepository.findByEmail(userDetails.getEmail());
        if (userByEmail != null && !userByEmail.getUsername().equals(username)) {
            throw new RuntimeException("User with this email already exists!");
        }
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

    /**
     * Retrieves a User entity by username within a transactional (read-only) context.
     * <p>
     * This method ensures that all lazily-loaded associations such as
     * {@code passengers} and {@code bookings} are initialized while the
     * Hibernate session is still active, preventing LazyInitializationException.
     * <p>
     * The {@code @Transactional(readOnly = true)} annotation maintains a read-only
     * session for efficient data retrieval without modifying the database.
     *
     * @param username the username of the user to retrieve
     * @return the fully initialized User entity with associated passengers and bookings
     */

    @Transactional(readOnly = true)
    public User findByUserName(String username) {
        User user = userRepository.findByUsername(username);

//         Force lazy collections to load before session closes
        user.getPassengers().size(); // initializes the passengers collection
        user.getBookings().size();   // initializes the bookings collection

        return userRepository.findByUsername(username);
    }



}