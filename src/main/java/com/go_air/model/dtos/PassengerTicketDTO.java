package com.go_air.model.dtos;

import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import com.go_air.enums.AircraftSize;
import com.go_air.enums.BookingStatus;
import com.go_air.enums.BookingType;
import com.go_air.enums.DepartureType;
import com.go_air.enums.JourneyStatus;
import com.go_air.enums.SeatType;
import com.go_air.enums.SpecialFareType;
import com.go_air.enums.TravelClass;
import com.go_air.enums.TripType;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PassengerTicketDTO {

    private Long ticketId;
    private String userId;                
    private String flightNumber;
    private TripType tripType;
    private AircraftSize aircraftSize;

    private LocalDate departureDate;
    private LocalTime departureTime;

    private LocalDate arrivalDate;
    private LocalTime arrivalTime;
    private LocalDateTime bookingTime;
    private String airline;                // Added
    private String sourceAirport;
    private String destinationAirport;
    private Integer stop;
    private String destinationStop;

    private BookingType bookingType;
    private DepartureType departureType;
    private Integer durationMinutes;
    private Integer price;                

    private Double totalAmount;
    private BookingStatus bookingStatus;  

    private SpecialFareType specialFareType;
    private JourneyStatus journeyStatus;
    private int passengerCount;

    // Passenger details
    private String passengerName;
    private String gender;
    private int age;
    private String passportNumber;
    private String seatNo;
    private SeatType seatType;
    private TravelClass travelClass;
}
