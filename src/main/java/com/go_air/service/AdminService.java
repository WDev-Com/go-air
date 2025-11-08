package com.go_air.service;

import com.go_air.entity.Flights;
import com.go_air.entity.Seat;
import com.go_air.enums.AircraftSize;
import com.go_air.enums.BookingType;
import com.go_air.enums.DepartureType;
import com.go_air.enums.JourneyStatus;
import com.go_air.enums.SeatOperationStatus;
import com.go_air.enums.SeatPosition;
import com.go_air.enums.SeatStatus;
import com.go_air.enums.SeatType;
import com.go_air.enums.SpecialFareType;
import com.go_air.enums.TravelClass;
import com.go_air.enums.TripType;
import com.go_air.repo.FlightRepository;
import com.go_air.repo.SeatRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class AdminService {

    @Autowired
    private FlightRepository flightRepo;
    @Autowired
    private  SeatRepository seatRepository;
    
    private static final Logger log = LoggerFactory.getLogger(AdminService.class);


    // Create flight
    public Flights createFlight(Flights flight) {
        return flightRepo.save(flight);
    }

    // Read all flights
    public List<Flights> getAllFlights() {
        return flightRepo.findAll();
    }

    // Get flight by flight number
    public Flights getFlightByFlightNumber(String flightNumber) {
        return flightRepo.findByFlightNumber(flightNumber).orElse(null);
    }

    // Update flight by flight number
    public Flights updateFlightByFlightNumber(String flightNumber, Flights updatedFlight) {
        Flights flight = flightRepo.findByFlightNumber(flightNumber).orElse(null);
        if (flight != null) {
            flight.setAirline(updatedFlight.getAirline());
            flight.setSourceAirport(updatedFlight.getSourceAirport());
            flight.setDestinationAirport(updatedFlight.getDestinationAirport());
            flight.setStop(updatedFlight.getStop());
            flight.setDestinationStop(updatedFlight.getDestinationStop());
            flight.setBookingType(updatedFlight.getBookingType());
            flight.setDepartureType(updatedFlight.getDepartureType());
            flight.setDepartureDate(updatedFlight.getDepartureDate());
            flight.setDepartureTime(updatedFlight.getDepartureTime());
            flight.setArrivalDate(updatedFlight.getArrivalDate());
            flight.setArrivalTime(updatedFlight.getArrivalTime());
            flight.setDurationMinutes(updatedFlight.getDurationMinutes());
            flight.setPrice(updatedFlight.getPrice());
            flight.setAvailableSeats(updatedFlight.getAvailableSeats());
            return flightRepo.save(flight);
        }
        return null;
    }

    // Delete flight by flight number
    public String deleteFlightByFlightNumber(String flightNumber) {
        Flights flight = flightRepo.findByFlightNumber(flightNumber).orElse(null);
        if (flight != null) {
            flightRepo.delete(flight);
            return "Flight with number " + flightNumber + " deleted successfully.";
        }
        return "Flight with number " + flightNumber + " not found.";
    }
    
    public Map<String, Object> searchFlightsWithPagination(
            List<String> airlines,
            String sourceAirport,
            String destinationAirport,
            LocalDate departureDate,
            LocalDate retDate,
            Integer stop,
            BookingType bookingType,
            DepartureType departureType,
            Integer minPrice,
            Integer maxPrice,
            AircraftSize aircraftSize,
            SpecialFareType specialFareType,
            int page,
            int limit
    ) {
        if (page < 0) page = 0;
        if (limit <= 0) limit = 5;

        List<String> validAirlines = (airlines != null && !airlines.isEmpty())
                ? airlines.stream().map(String::trim).filter(s -> !s.isEmpty()).toList()
                : null;

        List<Flights> flights = flightRepo.findFlightsByOptionalFilters(
                validAirlines,
                sourceAirport != null && !sourceAirport.isBlank() ? sourceAirport.trim() : null,
                destinationAirport != null && !destinationAirport.isBlank() ? destinationAirport.trim() : null,
                departureDate,
                stop,
                bookingType,       // pass enum directly
                departureType,     // pass enum directly
                minPrice,
                maxPrice,
                aircraftSize       // pass enum directly
        );

        // Apply special fare discount if provided
        if (specialFareType != null) {
            flights.forEach(f -> f.setPrice(specialFareType.applyDiscount(f.getPrice())));
        }

        // Remove duplicates
        flights = new ArrayList<>(new LinkedHashSet<>(flights));

        // Pagination
        int total = flights.size();
        int fromIndex = Math.min(page * limit, total);
        int toIndex = Math.min(fromIndex + limit, total);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("totalElements", total);
        response.put("page", page);
        response.put("limit", limit);
        response.put("flights", flights.subList(fromIndex, toIndex));

        return response;
    }


    // Extra Method For Experimantation
    public Map<String, Object> getAllFlightsPaginated(int page, int limit) {
        List<Flights> allFlights = flightRepo.findAll(); // assuming flightsRepository exists
        int total = allFlights.size();

        int fromIndex = page * limit;
        int toIndex = Math.min(fromIndex + limit, total);

        List<Flights> pagedFlights = allFlights.subList(
                Math.min(fromIndex, total),
                Math.min(toIndex, total)
        );

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("totalElements", total);
        response.put("page", page);
        response.put("limit", limit);
        response.put("flights", pagedFlights);

        return response;
    }

    @Transactional
    public SeatOperationStatus generateSeatsForFlight(String flightNo) {
        Flights flight = flightRepo.findByFlightNumber(flightNo)
                .orElseThrow(() -> new IllegalArgumentException("Flight not found with flight number: " + flightNo));

        // If journey is completed, don't allow seat creation or update
        if (flight.getJourneyStatus() == JourneyStatus.COMPLETED) {
            return SeatOperationStatus.COMPLETED_NO_CHANGE;
        }

        List<Seat> existingSeats = seatRepository.findSeatsByFlightNumber(flightNo);

        if (existingSeats.isEmpty()) {
            // ✅ Create new seats
            int totalSeats = flight.getAvailableSeats();
            AircraftSize size = flight.getAircraftSize();

            int columns = getColumnsByAircraftSize(size);  // seats per row
            int rows = (int) Math.ceil((double) totalSeats / columns);

            List<Seat> seats = new ArrayList<>();
            char[] seatLabels = generateSeatLabels(columns);

            for (int row = 1; row <= rows; row++) {
                for (int col = 0; col < columns; col++) {
                    String columnLabel = String.valueOf(seatLabels[col]);
                    String seatNumber = row + columnLabel;

                    Seat seat = Seat.builder()
                            .seatNumber(seatNumber)
                            .rowNumber(row)
                            .columnLabel(columnLabel)
                            .seatPosition(getSeatPosition(col, columns))
                            .seatType(getSeatType(col, columns))
                            .seatStatus(SeatStatus.AVAILABLE)
                            .travelClass(TravelClass.ECONOMY)
                            .flight(flight)
                            .build();

                    seats.add(seat);
                }
            }

            seatRepository.saveAll(seats);
            return SeatOperationStatus.CREATED;

        } else {
            // ✅ Seats already exist — only update their status
            for (Seat seat : existingSeats) {
                seat.setSeatStatus(SeatStatus.AVAILABLE);
            }
            seatRepository.saveAll(existingSeats);
            return SeatOperationStatus.UPDATED;
        }
    }



    private int getColumnsByAircraftSize(AircraftSize size) {
        return switch (size) {
            case LIGHT -> 4;   // 2 seats per side (A-B / C-D)
            case MEDIUM -> 6;  // 3 seats per side (A-B-C / D-E-F)
            case LARGE -> 8;   // 4 per side (A-B-C-D / E-F-G-H)
            case JUMBO -> 10;  // e.g., 3-4-3 layout (A-B-C / D-E-F-G / H-J-K)
        };
    }

    private char[] generateSeatLabels(int columns) {
        char[] labels = new char[columns];
        for (int i = 0; i < columns; i++) {
            labels[i] = (char) ('A' + i);
        }
        return labels;
    }

    private SeatPosition getSeatPosition(int col, int columns) {
        if (columns <= 4) {
            // 2-2 layout
            return (col < columns / 2) ? SeatPosition.LEFT : SeatPosition.RIGHT;
        } else if (columns == 6) {
            // 3-3 layout
            if (col <= 2) return SeatPosition.LEFT;
            else return SeatPosition.RIGHT;
        } else if (columns == 8) {
            // 4-4 layout
            if (col <= 3) return SeatPosition.LEFT;
            else return SeatPosition.RIGHT;
        } else {
            // 10 columns => 3-4-3 layout
            if (col <= 2) return SeatPosition.LEFT;
            else if (col <= 6) return SeatPosition.MIDDLE;
            else return SeatPosition.RIGHT;
        }
    }

    private SeatType getSeatType(int col, int columns) {
        if (col == 0 || col == columns - 1) return SeatType.WINDOW;

        // AISLE seats (edges between sections)
        if (columns == 4 && (col == 1 || col == 2)) return SeatType.AISLE;
        if (columns == 6 && (col == 2 || col == 3)) return SeatType.AISLE;
        if (columns == 8 && (col == 3 || col == 4)) return SeatType.AISLE;
        if (columns == 10 && (col == 2 || col == 3 || col == 6 || col == 7)) return SeatType.AISLE;

        return SeatType.MIDDLE;
    }
    
    /**
     * Fetch all seats for a specific flight number.
     *
     * @param flightNumber The flight number (e.g., "GA102")
     * @return List of Seat entities
     */
    public List<Seat> getSeatsByFlightNumber(String flightNumber) {
        return seatRepository.findSeatsByFlightNumber(flightNumber);
    }
    
    @Transactional
    public void updateJourneyStatuses() {
        List<Flights> flights = flightRepo.findAll();

        LocalDateTime now = LocalDateTime.now();

        for (Flights flight : flights) {
            LocalDateTime departure = LocalDateTime.of(flight.getDepartureDate(), flight.getDepartureTime());
            LocalDateTime arrival = LocalDateTime.of(flight.getArrivalDate(), flight.getArrivalTime());

            JourneyStatus currentStatus;
            if (now.isBefore(departure)) {
                currentStatus = JourneyStatus.SCHEDULED;
            } else if (now.isAfter(arrival) || now.isEqual(arrival)) {
                currentStatus = JourneyStatus.COMPLETED;
            } else {
                currentStatus = JourneyStatus.IN_PROGRESS;
            }

            if (flight.getJourneyStatus() != currentStatus) {
                flight.setJourneyStatus(currentStatus);
                flightRepo.save(flight);
            }
        }
    }
    
    /* Experimental Code */
    public String getSeatLayoutText(String flightNumber) {
        Flights flight = flightRepo.findAll().stream()
                .filter(f -> f.getFlightNumber().equalsIgnoreCase(flightNumber))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Flight not found: " + flightNumber));

        List<Seat> seats = seatRepository.findSeatsByFlightNumber(flightNumber);
        if (seats.isEmpty()) {
            return "No seats found for flight " + flightNumber;
        }

        // Group seats by row
        Map<Integer, List<Seat>> rows = seats.stream()
                .collect(Collectors.groupingBy(Seat::getRowNumber, LinkedHashMap::new, Collectors.toList()));

        StringBuilder layout = new StringBuilder();
        layout.append("Flight: ").append(flightNumber).append("\n");
        layout.append("Aircraft Size: ").append(flight.getAircraftSize()).append("\n");
        layout.append("---------------------------------\n");

        for (Map.Entry<Integer, List<Seat>> entry : rows.entrySet()) {
            int rowNumber = entry.getKey();
            List<Seat> rowSeats = entry.getValue().stream()
                    .sorted(Comparator.comparing(Seat::getColumnLabel))
                    .collect(Collectors.toList());

            layout.append(String.format("%-3d:  ", rowNumber));
            layout.append(formatRow(rowSeats, flight.getAircraftSize()));
            layout.append("\n");
        }

        return layout.toString();
    }

    private String formatRow(List<Seat> seats, AircraftSize size) {
        // Sort seats by label
        seats.sort(Comparator.comparing(Seat::getColumnLabel));

        // Convert each seat to short text form
        List<String> seatBlocks = seats.stream()
                .map(s -> {
                    String symbol = switch (s.getSeatStatus()) {
                        case AVAILABLE -> "[ ]";
                        case OCCUPIED -> "[X]";
                        case RESERVED -> "[R]";
                        case BLOCKED -> "[-]";
                    };
                    return symbol.replace(" ", s.getColumnLabel());
                })
                .collect(Collectors.toList());

        return switch (size) {
            case LIGHT -> seatBlocks.get(0) + seatBlocks.get(1) + "   " + seatBlocks.get(2) + seatBlocks.get(3);
            case MEDIUM -> String.join("", seatBlocks.subList(0, 3)) + "   " + String.join("", seatBlocks.subList(3, 6));
            case LARGE -> {
                // For large: left 2 seats, middle 4 seats (double), right 2 seats
                String left = String.join("", seatBlocks.subList(0, 2));
                String middle = String.join("", seatBlocks.subList(2, 6));
                String right = String.join("", seatBlocks.subList(6, 8));
                yield left + "   " + middle + "   " + right;
            }
            case JUMBO -> {
                // Example: left 3, middle 4, right 3
                String left = String.join("", seatBlocks.subList(0, 3));
                String middle = String.join("", seatBlocks.subList(3, 7));
                String right = String.join("", seatBlocks.subList(7, 10));
                yield left + "   " + middle + "   " + right;
            }
        };
    }

}
