package com.go_air.model.dtos;

import com.go_air.enums.AircraftSize;
import com.go_air.enums.BookingStatus;
import com.go_air.enums.BookingType;
import com.go_air.enums.DepartureType;
import com.go_air.enums.JourneyStatus;
import com.go_air.enums.SpecialFareType;
import com.go_air.enums.TripType;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingResponseDTO {

    private Long id;
    private String flightNumber;
    private TripType tripType;
    private AircraftSize aircraftSize;
    private SpecialFareType specialFareType;
    private JourneyStatus journeyStatus;
    private BookingStatus status;
    
    private LocalDate departureDate;
    private LocalTime departureTime;
    private LocalDate arrivalDate;
    private LocalTime arrivalTime;
    private LocalDateTime bookingTime;

    private String airline;
    private String sourceAirport;
    private String destinationAirport;
    private Integer stop;
    private String destinationStop;

    private BookingType bookingType;
    private DepartureType departureType;
    private Integer durationMinutes;
    private Integer price;

    private int passengerCount;
    private double totalAmount;
    
    private List<PassengerResponseDTO> passengers;
}
