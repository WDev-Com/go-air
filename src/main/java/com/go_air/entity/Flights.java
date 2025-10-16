package com.go_air.entity;

import java.time.LocalDate;
import java.time.LocalTime;
import com.go_air.enums.BookingType;
import com.go_air.enums.DepartureType;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@Entity
@Table(
    name = "flights",
    uniqueConstraints = @UniqueConstraint(
        columnNames = {"flightNumber"}
    )
)
public class Flights {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String airline;
    @Column(nullable = false, unique = true) 
    private String flightNumber;
    private String sourceAirport;
    private String destinationAirport;
    private int stop;
    private String destinationStop;

    @Enumerated(EnumType.STRING)
    private BookingType bookingType;

    @Enumerated(EnumType.STRING)
    private DepartureType departureType;

    private LocalDate departureDate;
    @Column(columnDefinition = "TIME WITHOUT TIME ZONE")
    private LocalTime departureTime;

    private LocalDate arrivalDate;
    @Column(columnDefinition = "TIME WITHOUT TIME ZONE")
    private LocalTime arrivalTime;

    private int durationMinutes;
    private double price;
    private int availableSeats;
}
