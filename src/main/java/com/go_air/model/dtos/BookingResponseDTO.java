package com.go_air.model.dtos;

import com.go_air.enums.BookingStatus;
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
    private LocalDate departureDate;
    private LocalTime departureTime;
    private LocalDate arrivalDate;
    private LocalTime arrivalTime;
    private LocalDateTime bookingTime;
    private int passengerCount;
    private double totalAmount;
    private BookingStatus status;
    private List<PassengerResponseDTO> passengers;
}
