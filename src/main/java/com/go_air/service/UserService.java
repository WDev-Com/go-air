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
import com.go_air.enums.PaymentStatus;
import com.go_air.enums.SeatStatus;
import com.go_air.enums.SpecialFareType;
import com.go_air.enums.TripType;
import com.go_air.model.dtos.BookingResponseDTO;
import com.go_air.model.dtos.PassengerResponseDTO;
import com.go_air.model.dtos.PassengerTicketDTO;
import com.go_air.repo.BookingRepository;
import com.go_air.repo.FlightRepository;
import com.go_air.repo.SeatRepository;
import com.go_air.repo.UserRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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
    private UserRepository userRepository;

    @Autowired
    private FlightRepository flightRepository;

    @Autowired
    private AdminService adminService; // fetch flight info

    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private SeatRepository seatRepository;
    
    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    
    public List<String> getAirportSuggestions(String type, String query) {

        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }

        if (type.equalsIgnoreCase("source")) {
            return flightRepository.searchSourceAirports(query);
        } 
        else if (type.equalsIgnoreCase("destination")) {
            return flightRepository.searchDestinationAirports(query);
        }

        return List.of(); // invalid type
    }

    
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

    public String generateUniqueBookingNumber() {

        String timestamp = String.valueOf(System.currentTimeMillis()); // 13 digits
        
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder randomPart = new StringBuilder();
        java.util.Random random = new java.util.Random();

        for (int i = 0; i < 6; i++) { // 6 random characters
            randomPart.append(chars.charAt(random.nextInt(chars.length())));
        }

        return "BK" + timestamp + randomPart.toString();
    }


    // Book a flight for a user with passengers
    @Transactional
    public Booking bookFlight(String userId, Booking bookingRequest) {
        // 1️ User
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 2️ Flight
        Flights flight = flightRepository.findByFlightNumber(bookingRequest.getFlightNumber())
                .orElseThrow(() -> new RuntimeException("Flight not found"));

        LocalDate depDate = flight.getDepartureDate();
        LocalTime depTime = flight.getDepartureTime();
        LocalDate arrDate = flight.getArrivalDate();
        LocalTime arrTime = flight.getArrivalTime();

        List<Passenger> passengers = bookingRequest.getPassengers();
        if (passengers == null || passengers.isEmpty()) {
            throw new RuntimeException("No passengers provided for booking");
        }

        double totalAmount = 0.0;

        for (Passenger passenger : passengers) {
            Seat seat = seatRepository.findSeatsByFlightNumber(flight.getFlightNumber()).stream()
                    .filter(s -> s.getSeatNumber().equalsIgnoreCase(passenger.getSeatNo()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Seat " + passenger.getSeatNo() + " not found"));

            if (seat.getSeatStatus() != SeatStatus.AVAILABLE) {
                throw new RuntimeException("Seat " + passenger.getSeatNo() + " is already booked");
            }

            boolean alreadyBooked = bookingRepo.existsByFlightNumberAndUser_UserIDAndPassengers_Name(
                    flight.getFlightNumber(), userId, passenger.getName());
            if (alreadyBooked) {
                throw new RuntimeException("Passenger " + passenger.getName() +
                        " already booked on flight " + flight.getFlightNumber());
            }

            boolean hasConflict = bookingRepo.existsBookingConflictByPassport(
                    passenger.getPassportNumber(), depDate, depTime, arrDate, arrTime);
            if (hasConflict) {
                throw new RuntimeException("Passenger " + passenger.getName() +
                        " with passport " + passenger.getPassportNumber() +
                        " has another overlapping booking.");
            }

            passenger.setTravelClass(seat.getTravelClass());
            passenger.setSeatType(seat.getSeatType());
            passenger.setDepartureType(flight.getDepartureType());
            passenger.setUser(user);

            double baseFare = flight.getPrice();
            switch (seat.getTravelClass()) {
                case PREMIUM_ECONOMY -> baseFare *= 1.3;
                case BUSINESS -> baseFare *= 1.8;
                case FIRST_CLASS -> baseFare *= 2.5;
                default -> baseFare *= 1.0;
            }

            totalAmount += baseFare;

            seat.setSeatStatus(SeatStatus.AVAILABLE);
            seatRepository.save(seat);
        }

        // Special fare
        SpecialFareType fareType = bookingRequest.getSpecialFareType();
        if (fareType == null) fareType = SpecialFareType.REGULAR;

        fareType.validatePassengers(passengers.size());
        double totalDiscount = (flight.getPrice() - fareType.applyDiscount(flight.getPrice())) * passengers.size();
        double finalAmount = Math.max(totalAmount - totalDiscount, 0);

        bookingRequest.setUser(user);
        bookingRequest.setPassengers(passengers);
        bookingRequest.setBookingTime(LocalDateTime.now());
        bookingRequest.setPassengerCount(passengers.size());
        bookingRequest.setStatus(BookingStatus.PENDING);
        bookingRequest.setJourneyStatus(JourneyStatus.SCHEDULED);
        bookingRequest.setSpecialFareType(fareType);
        bookingRequest.setTotalAmount(finalAmount);
        bookingRequest.setAircraftSize(flight.getAircraftSize());
        bookingRequest.setTripType(bookingRequest.getTripType());
        bookingRequest.setDepartureDate(depDate);
        bookingRequest.setDepartureTime(depTime);
        bookingRequest.setArrivalDate(arrDate);
        bookingRequest.setArrivalTime(arrTime);

        Booking savedBooking = bookingRepo.save(bookingRequest);

        flight.setAvailableSeats(flight.getAvailableSeats() - passengers.size());
        flightRepository.save(flight);

        return savedBooking;
    }
    
    public List<Booking> getBookingsByBookingNo(String bookingNo) {

        if (bookingNo == null || bookingNo.isEmpty()) {
            throw new IllegalArgumentException("Booking number cannot be empty");
        }

        return bookingRepo.findByBookingNo(bookingNo);
    }

    
    // ✅ Update all pending bookings of a user to a new status
    @Transactional
    public void updateBookingAfterPayment(String userId,
                                          BookingStatus bookingStatus,
                                          PaymentStatus paymentStatus,
                                          String paymentId) {

        List<Booking> pendingBookings =
                bookingRepo.findByUser_UserIDAndStatusWithPassengers(userId, BookingStatus.PENDING);

        if (pendingBookings.isEmpty()) return;

        for (Booking booking : pendingBookings) {

            booking.setStatus(bookingStatus);
            booking.setPaymentStatus(paymentStatus);
            booking.setPaymentID(paymentId);
            bookingRepo.save(booking);

            // only when confirmed
            log.info("bookingStatus == BookingStatus.CONFIRMED : "+(bookingStatus == BookingStatus.CONFIRMED));
            if (bookingStatus == BookingStatus.CONFIRMED) {

                // UPDATE SEAT STATUS
                for (Passenger passenger : booking.getPassengers()) {
                    Seat seat = seatRepository
                            .findByFlight_FlightNumberAndSeatNumber(
                                    booking.getFlightNumber(),
                                    passenger.getSeatNo()
                            )
                            .orElseThrow(() -> new RuntimeException(
                                    "Seat " + passenger.getSeatNo() +
                                            " not found on flight " + booking.getFlightNumber()));

                    seat.setSeatStatus(SeatStatus.OCCUPIED);
                    seatRepository.save(seat);
                }

                // UPDATE AVAILABLE SEATS
                Flights flight = flightRepository.findByFlightNumber(booking.getFlightNumber())
                        .orElseThrow(() -> new RuntimeException("Flight not found: " + booking.getFlightNumber()));

                flight.setAvailableSeats(flight.getAvailableSeats() - booking.getPassengerCount());
                flightRepository.save(flight);
            }
        }
    }



    // Cancel booking
    // Cancel booking
    public Map<String, Object> cancelBooking(String bookingNo) {

        // ✅ Fetch all bookings for the booking number (ONE_WAY, ROUND_TRIP, MULTI_CITY)
        List<Booking> bookings = bookingRepo.findByBookingNo(bookingNo);

        if (bookings == null || bookings.isEmpty()) {
            throw new RuntimeException("Booking not found");
        }

        // ✅ Prepare response map
        Map<String, Object> response = new HashMap<>();
        List<Map<String, String>> flightResponses = new ArrayList<>();

        // ✅ Loop through each flight under the same booking number
        for (Booking booking : bookings) {
            String flightNumber = booking.getFlightNumber();

            Flights flight = flightRepository.findByFlightNumber(flightNumber)
                    .orElseThrow(() -> new RuntimeException("Flight not found for flight number: " + flightNumber));

            Map<String, String> flightResponse = new HashMap<>();
            flightResponse.put("bookingId", String.valueOf(booking.getId()));
            flightResponse.put("flightNumber", flight.getFlightNumber());

            // ✅ Check if flight is already cancelled
            if (booking.getStatus() == BookingStatus.CANCELLED) {
                flightResponse.put("status", BookingStatus.CANCELLED.toString());
                flightResponse.put("message", String.format(
                        "This flight (%s) has already been cancelled earlier.",
                        flightNumber));
                flightResponses.add(flightResponse);
                continue; // move to next flight
            }

            // ✅ Proceed to cancel
            booking.setStatus(BookingStatus.CANCELLED);
            bookingRepo.save(booking);
            flightResponse.put("status", booking.getStatus().toString());

            // ✅ Case 1: NON-REFUNDABLE
            if (flight.getBookingType() == BookingType.NON_REFUNDABLE) {
                flightResponse.put("message",
                        "Your booking for this flight has been cancelled successfully. This ticket was NON-REFUNDABLE, so no refund will be issued.");
                flightResponses.add(flightResponse);
                continue;
            }

            // ✅ Case 2: REFUNDABLE
            Double totalAmount = booking.getTotalAmount();
            int baseCancellationCharge = flight.getCancellationCharges(); // %

            LocalDate departureDate = flight.getDepartureDate();
            LocalTime departureTime = flight.getDepartureTime();
            LocalDateTime departureDateTime = LocalDateTime.of(departureDate, departureTime);
            LocalDateTime now = LocalDateTime.now();

            long hoursBeforeDeparture = Duration.between(now, departureDateTime).toHours();
            int finalCancellationPercent;

            if (hoursBeforeDeparture > 48) {
                finalCancellationPercent = baseCancellationCharge / 2; // before 2 days
            } else if (hoursBeforeDeparture >= 24) {
                finalCancellationPercent = baseCancellationCharge; // 24–48 hours
            } else if (hoursBeforeDeparture < 4) {
                finalCancellationPercent = (int) Math.round(baseCancellationCharge * 1.2); // +20%
            } else {
                finalCancellationPercent = baseCancellationCharge; // default case
            }

            if (finalCancellationPercent > 100) finalCancellationPercent = 100;

            double refundAmount = totalAmount - ((finalCancellationPercent / 100.0) * totalAmount);

            flightResponse.put("refundAmount", String.format("%.2f", refundAmount));
            flightResponse.put("cancellationCharges", finalCancellationPercent + "%");
            flightResponse.put("message", String.format(
                    "Your booking for flight %s has been cancelled successfully. %.2f will be credited to your account within 2-3 business days (after deducting %d%% cancellation charges).",
                    flightNumber, refundAmount, finalCancellationPercent));

            flightResponses.add(flightResponse);
        }

        // ✅ Return combined response
        response.put("bookingNo", bookingNo);
        response.put("status", "CANCELLED");
        response.put("flights", flightResponses);
        response.put("message", "Cancellation request processed successfully.");

        return response;
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
                            .bookingNo(booking.getBookingNo())
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

    

    
	public List<Seat> getSeatsByFlightNumber(String flightNumber) {
        log.info("UserService Line no 808 | Flight No : {}",flightNumber);
		// TODO Auto-generated method stub
        List<Seat> seats = seatRepository.findSeatsByFlightNumber(flightNumber);
//        log.info("Seats--------------------->>>{}",seats);
        
        
//        List<Seat> allseats = seatRepository.findAllSeats();
//        log.info("Seats--------------------->>>{}",allseats.size());
//        
        return seats;
	}



}