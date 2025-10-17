package com.go_air.model.dtos;

import lombok.*;
import java.time.LocalDate;
import java.time.LocalTime;

import com.go_air.enums.BookingStatus;
import com.go_air.enums.BookingType;
import com.go_air.enums.DepartureType;
import com.go_air.enums.TripType;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PassengerTicketDTO {

    private Long ticketId;
    private String flightNumber;
    private TripType tripType;

    private LocalDate departureDate;
    private LocalTime departureTime;

    private LocalDate arrivalDate;
    private LocalTime arrivalTime;

    private String sourceAirport;
    private String destinationAirport;
    private Integer stop;
    private String destinationStop;

    private BookingType bookingType;
    private DepartureType departureType;
    private Integer durationMinutes;

    private Double totalAmount;
    private BookingStatus status;

    private String passengerName;
    private String gender;
    private int age;
}


    



