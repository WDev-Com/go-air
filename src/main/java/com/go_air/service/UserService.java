package com.go_air.service;

import com.go_air.entity.Booking;
import com.go_air.entity.Flights;
import com.go_air.entity.Passenger;
import com.go_air.entity.Seat;
import com.go_air.entity.User;
import com.go_air.enums.AircraftSize;
import com.go_air.enums.BookingStatus;
import com.go_air.enums.BookingType;
import com.go_air.enums.DepartureType;
import com.go_air.enums.JourneyStatus;
import com.go_air.enums.SeatStatus;
import com.go_air.enums.SpecialFareType;
import com.go_air.enums.TripType;
import com.go_air.model.dtos.BookingResponseDTO;
import com.go_air.model.dtos.PassengerResponseDTO;
import com.go_air.model.dtos.PassengerTicketDTO;
import com.go_air.repo.BookingRepository;
import com.go_air.repo.FlightRepository;
import com.go_air.repo.PassengerRepository;
import com.go_air.repo.SeatRepository;
import com.go_air.repo.UserRepository;



import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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
    
    @Autowired
    private SeatRepository seatRepository;
    
    public Map<String, List<Flights>> searchFlightsByTripType(
            TripType tripType,
            List<String> airlines,
            List<String> sourceAirports,
            List<String> destinationAirports,
            List<LocalDate> departureDates, // <-- accepts as String list
            LocalDate retDate,
            Integer stop,
            BookingType bookingType,
            DepartureType departureType,
            Integer minPrice,
            Integer maxPrice,
            Integer passengers,
            AircraftSize aircraftSize,
            SpecialFareType specialFareType
    ) {
        Map<String, List<Flights>> result = new LinkedHashMap<>();

        if (sourceAirports == null || destinationAirports == null
                || sourceAirports.isEmpty() || destinationAirports.isEmpty()) {
            throw new IllegalArgumentException("Source and destination airports must not be empty for the selected trip type.");
        }

        List<String> validAirlines = (airlines != null)
                ? airlines.stream().map(String::trim).filter(s -> !s.isEmpty()).toList()
                : List.of();

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
                        		validAirlines, params.src(), params.dest(), params.date(),
                                stop, bookingType, departureType,
                                minPrice, adjustedMaxPrice, passengers,aircraftSize
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

                // Forward flight search
                result.put(forwardKey, fetchAndDiscount.apply(new FlightParams(
                        validSources.get(0),
                        validDestinations.get(0),
                        departureDates.get(0)
                )));

                // Determine return date
                LocalDate returnDate;
                if (retDate != null) {
                    // User provided both dates
                    returnDate = retDate;
                } else {
                    // Default to next day if return date not provided
                    returnDate = departureDates.get(0).plusDays(1);
                }

                // Fetch return flights
                var returnFlights = fetchAndDiscount.apply(new FlightParams(
                        validDestinations.get(0),
                        validSources.get(0),
                        returnDate
                ));

                // If no flights found, throw an exception
                if (returnFlights == null || returnFlights.isEmpty()) {
                    throw new RuntimeException("Flight not available for return date: " + returnDate);
                }

                // Otherwise, store results
                result.put(returnKey, returnFlights);
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
        List<String> airlines,
        String sourceAirport,
        String destinationAirport,
        LocalDate departureDate,
        Integer stop,
        BookingType bookingType,
        DepartureType departureType,
        Integer minPrice,
        Integer maxPrice,
        Integer passengers,
        AircraftSize aircraftSize
        
) {
   
    sourceAirport = (sourceAirport != null && !sourceAirport.trim().isEmpty()) ? sourceAirport.trim() : null;
    destinationAirport = (destinationAirport != null && !destinationAirport.trim().isEmpty()) ? destinationAirport.trim() : null;

    String bookingTypeStr = (bookingType != null) ? bookingType.name() : null;
    String departureTypeStr = (departureType != null) ? departureType.name() : null;
    String aircraftSizeStr = (aircraftSize != null) ? aircraftSize.name() : null;

    List<String> validAirlines = (airlines != null)
            ? airlines.stream().map(String::trim).filter(s -> !s.isEmpty()).toList()
            : List.of();

    
    
    return flightRepository.findFlightsByFilters(
    		validAirlines,
            sourceAirport,
            destinationAirport,
            departureDate,
            stop,
            bookingTypeStr,
            departureTypeStr,
            minPrice,
            maxPrice,
            passengers,
            aircraftSizeStr
    );
    }


    // Book a flight for a user with passengers
    @Transactional
    public Booking bookFlight(String userId, Booking bookingRequest) {
        // 1️ Check if user exists
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 2️ Fetch flight info
        Flights flight = flightRepository.findByFlightNumber(bookingRequest.getFlightNumber())
                .orElseThrow(() -> new RuntimeException("Flight not found"));

        LocalDate depDate = flight.getDepartureDate();
        LocalTime depTime = flight.getDepartureTime();
        LocalDate arrDate = flight.getArrivalDate();
        LocalTime arrTime = flight.getArrivalTime();

        // 3️ Validate passengers and seats
        List<Passenger> passengers = bookingRequest.getPassengers();
        if (passengers == null || passengers.isEmpty()) {
            throw new RuntimeException("No passengers provided for booking");
        }

        double totalAmount = 0.0;

        for (Passenger passenger : passengers) {
            // Find seat by seat number
            Seat seat = seatRepository.findSeatsByFlightNumber(flight.getFlightNumber()).stream()
                    .filter(s -> s.getSeatNumber().equalsIgnoreCase(passenger.getSeatNo()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Seat " + passenger.getSeatNo() + " not found on flight"));

            // Check seat availability
            if (seat.getSeatStatus() != SeatStatus.AVAILABLE) {
                throw new RuntimeException("Seat " + passenger.getSeatNo() + " is already booked");
            }

            // Prevent duplicate booking for same user + flight + passenger
            boolean alreadyBooked = bookingRepo.existsByFlightNumberAndUser_UserIDAndPassengers_Name(
                    flight.getFlightNumber(), userId, passenger.getName());
            if (alreadyBooked) {
                throw new RuntimeException("Passenger " + passenger.getName() +
                        " already booked on flight " + flight.getFlightNumber());
            }

            // Check for overlapping flight (passport conflict)
            boolean hasConflict = bookingRepo.existsBookingConflictByPassport(
                    passenger.getPassportNumber(), depDate, depTime, arrDate, arrTime);
            if (hasConflict) {
                throw new RuntimeException("Passenger " + passenger.getName() +
                        " with passport " + passenger.getPassportNumber() +
                        " has another overlapping booking.");
            }

            // Assign class/type details
            passenger.setTravelClass(seat.getTravelClass());
            passenger.setSeatType(seat.getSeatType());
            passenger.setDepartureType(flight.getDepartureType());
            passenger.setUser(user);

            // Calculate base fare depending on class
            double baseFare = flight.getPrice();
            switch (seat.getTravelClass()) {
                case PREMIUM_ECONOMY -> baseFare *= 1.3;
                case BUSINESS -> baseFare *= 1.8;
                case FIRST -> baseFare *= 2.5;
                default -> baseFare *= 1.0;
            }
            
            totalAmount += baseFare;

            // Mark seat as occupied
            seat.setSeatStatus(SeatStatus.OCCUPIED);
            seatRepository.save(seat);
        }

        // 4️ Apply special fare discounts
        SpecialFareType fareType = bookingRequest.getSpecialFareType();
        if (fareType == null) {
            fareType = SpecialFareType.REGULAR;
        }

        // Validate minimum passenger requirement
        fareType.validatePassengers(passengers.size());

        // Apply fixed discount logic from enum
        int flightPrice = flight.getPrice() != null ? flight.getPrice() : 10000;
        int discountedPrice = fareType.applyDiscount(flightPrice);
        double discountPerPassenger = flightPrice - discountedPrice;

        // Apply total discount
        double totalDiscount = discountPerPassenger * passengers.size();
        double finalAmount = totalAmount - totalDiscount;

        // Ensure final amount not below zero
        if (finalAmount < 0) finalAmount = 0;

        // 5️ Prepare booking
        bookingRequest.setUser(user);
        bookingRequest.setPassengers(passengers);
        bookingRequest.setBookingTime(LocalDateTime.now());
        bookingRequest.setPassengerCount(passengers.size());
        bookingRequest.setStatus(BookingStatus.CONFIRMED);
        bookingRequest.setJourneyStatus(JourneyStatus.SCHEDULED);
        bookingRequest.setSpecialFareType(fareType);
        bookingRequest.setTotalAmount(finalAmount);

        // Default flight details
        bookingRequest.setAircraftSize(flight.getAircraftSize());
        bookingRequest.setTripType(bookingRequest.getTripType());
        bookingRequest.setDepartureDate(depDate);
        bookingRequest.setDepartureTime(depTime);
        bookingRequest.setArrivalDate(arrDate);
        bookingRequest.setArrivalTime(arrTime);

        // 6️ Save booking
        Booking savedBooking = bookingRepo.save(bookingRequest);

        // 7️ Update available seats
        int remainingSeats = flight.getAvailableSeats() - passengers.size();
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
        
    // Generate Tickets By User
    }

   // Get Tickets 
    public List<PassengerTicketDTO> generateTicketsByUser(String userId) {
        List<Booking> userBookings = bookingRepo.findByUser_UserID(userId);
        List<PassengerTicketDTO> tickets = new ArrayList<>();

        for (Booking booking : userBookings) {
            Flights flight = adminService.getFlightByFlightNumber(booking.getFlightNumber());

            for (Passenger passenger : booking.getPassengers()) {
                PassengerTicketDTO ticket = PassengerTicketDTO.builder()
                        .ticketId(booking.getId())
                        .userId(userId)
                        .flightNumber(booking.getFlightNumber())
                        .tripType(booking.getTripType())
                        .specialFareType(booking.getSpecialFareType())
                        .journeyStatus(booking.getJourneyStatus())
                        .bookingStatus(booking.getStatus())
                        .bookingTime(booking.getBookingTime())
                        .aircraftSize(booking.getAircraftSize())
                        .passengerCount(booking.getPassengerCount())
                        .totalAmount(booking.getPassengerCount() > 0
                                ? booking.getTotalAmount() / booking.getPassengerCount()
                                : booking.getTotalAmount())

                        // Flight info
                        .airline(flight != null ? flight.getAirline() : null)
                        .sourceAirport(flight != null ? flight.getSourceAirport() : null)
                        .destinationAirport(flight != null ? flight.getDestinationAirport() : null)
                        .departureDate(flight != null ? flight.getDepartureDate() : booking.getDepartureDate())
                        .departureTime(flight != null ? flight.getDepartureTime() : booking.getDepartureTime())
                        .arrivalDate(flight != null ? flight.getArrivalDate() : booking.getArrivalDate())
                        .arrivalTime(flight != null ? flight.getArrivalTime() : booking.getArrivalTime())
                        .stop(flight != null ? flight.getStop() : null)
                        .destinationStop(flight != null ? flight.getDestinationStop() : null)
                        .bookingType(flight != null ? flight.getBookingType() : null)
                        .departureType(flight != null ? flight.getDepartureType() : null)
                        .durationMinutes(flight != null ? flight.getDurationMinutes() : null)
                        .price(flight != null ? flight.getPrice() : null)

                        // Passenger info
                        .passengerName(passenger.getName())
                        .gender(passenger.getGender() != null ? passenger.getGender().toString() : null)
                        .age(passenger.getAge())
                        .passportNumber(passenger.getPassportNumber())
                        .seatNo(passenger.getSeatNo())
                        .seatType(passenger.getSeatType())
                        .travelClass(passenger.getTravelClass())
                        .build();

                tickets.add(ticket);
            }
        }

        return tickets;
    }

    public List<PassengerTicketDTO> generateTicketsByUserAndBooking(String userId, Long bookingId) {
        // Get the specific booking for the user
        Optional<Booking> bookingOpt = bookingRepo.findById(bookingId);

        if (bookingOpt.isEmpty()) {
            return Collections.emptyList(); // or throw custom exception
        }

        Booking booking = bookingOpt.get();

        // Ensure this booking belongs to the given user
        if (!booking.getUser().getUserID().equals(userId)) {
            return Collections.emptyList(); // or throw UnauthorizedException
        }

        Flights flight = adminService.getFlightByFlightNumber(booking.getFlightNumber());
        List<PassengerTicketDTO> tickets = new ArrayList<>();

        for (Passenger passenger : booking.getPassengers()) {
            PassengerTicketDTO ticket = PassengerTicketDTO.builder()
                    .ticketId(booking.getId())
                    .userId(userId)
                    .flightNumber(booking.getFlightNumber())
                    .tripType(booking.getTripType())
                    .specialFareType(booking.getSpecialFareType())
                    .journeyStatus(booking.getJourneyStatus())
                    .bookingStatus(booking.getStatus())
                    .bookingTime(booking.getBookingTime())
                    .aircraftSize(booking.getAircraftSize())
                    .passengerCount(booking.getPassengerCount())
                    .totalAmount(booking.getPassengerCount() > 0
                            ? booking.getTotalAmount() / booking.getPassengerCount()
                            : booking.getTotalAmount())

                    // Flight info
                    .airline(flight != null ? flight.getAirline() : null)
                    .sourceAirport(flight != null ? flight.getSourceAirport() : null)
                    .destinationAirport(flight != null ? flight.getDestinationAirport() : null)
                    .departureDate(flight != null ? flight.getDepartureDate() : booking.getDepartureDate())
                    .departureTime(flight != null ? flight.getDepartureTime() : booking.getDepartureTime())
                    .arrivalDate(flight != null ? flight.getArrivalDate() : booking.getArrivalDate())
                    .arrivalTime(flight != null ? flight.getArrivalTime() : booking.getArrivalTime())
                    .stop(flight != null ? flight.getStop() : null)
                    .destinationStop(flight != null ? flight.getDestinationStop() : null)
                    .bookingType(flight != null ? flight.getBookingType() : null)
                    .departureType(flight != null ? flight.getDepartureType() : null)
                    .durationMinutes(flight != null ? flight.getDurationMinutes() : null)
                    .price(flight != null ? flight.getPrice() : null)

                    // Passenger info
                    .passengerName(passenger.getName())
                    .gender(passenger.getGender() != null ? passenger.getGender().toString() : null)
                    .age(passenger.getAge())
                    .passportNumber(passenger.getPassportNumber())
                    .seatNo(passenger.getSeatNo())
                    .seatType(passenger.getSeatType())
                    .travelClass(passenger.getTravelClass())
                    .build();

            tickets.add(ticket);
        }

        return tickets;
    }


    //By Booking ID Update this function TO_DO
    public List<BookingResponseDTO> getBookingsByUser(String userId) {
        List<Booking> userBookings = bookingRepo.findByUser_UserID(userId);

        return userBookings.stream()
                .map((Booking booking) -> {
                    Flights flight = adminService.getFlightByFlightNumber(booking.getFlightNumber());

                    // Map passengers explicitly
                    List<PassengerResponseDTO> passengerDTOs = booking.getPassengers().stream()
                            .map((Passenger p) -> PassengerResponseDTO.builder()
                                    .passengerId(p.getId())
                                    .name(p.getName())
                                    .age(p.getAge())
                                    .gender(p.getGender())
                                    .passportNumber(p.getPassportNumber())
                                    .seatNo(p.getSeatNo())
                                    .seatType(p.getSeatType())
                                    .travelClass(p.getTravelClass())
                                    .departureType(p.getDepartureType())
                                    .build())
                            .collect(Collectors.toList());

                    return BookingResponseDTO.builder()
                            .id(booking.getId())
                            .flightNumber(booking.getFlightNumber())
                            .tripType(booking.getTripType())
                            .aircraftSize(booking.getAircraftSize())
                            .specialFareType(booking.getSpecialFareType())
                            .journeyStatus(booking.getJourneyStatus())
                            .status(booking.getStatus())
                            .bookingTime(booking.getBookingTime())
                            .passengerCount(booking.getPassengerCount())
                            .totalAmount(booking.getTotalAmount())

                            // Flight info
                            .airline(flight != null ? flight.getAirline() : null)
                            .sourceAirport(flight != null ? flight.getSourceAirport() : null)
                            .destinationAirport(flight != null ? flight.getDestinationAirport() : null)
                            .departureDate(flight != null ? flight.getDepartureDate() : booking.getDepartureDate())
                            .departureTime(flight != null ? flight.getDepartureTime() : booking.getDepartureTime())
                            .arrivalDate(flight != null ? flight.getArrivalDate() : booking.getArrivalDate())
                            .arrivalTime(flight != null ? flight.getArrivalTime() : booking.getArrivalTime())
                            .stop(flight != null ? flight.getStop() : null)
                            .destinationStop(flight != null ? flight.getDestinationStop() : null)
                            .bookingType(flight != null ? flight.getBookingType() : null)
                            .departureType(flight != null ? flight.getDepartureType() : null)
                            .durationMinutes(flight != null ? flight.getDurationMinutes() : null)
                            .price(flight != null ? flight.getPrice() : null)

                            // Passengers
                            .passengers(passengerDTOs)
                            .build();
                })
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