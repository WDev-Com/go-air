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
    @Column(name = "airline")
    private String airline;
    @Column(nullable = false, unique = true) 
    private String flightNumber;
    @Column(name = "source_airport")
    private String sourceAirport;
    @Column(name = "destination_airport")
    private String destinationAirport;
    private int stop;
    private String destinationStop;

    @Enumerated(EnumType.STRING)
    private BookingType bookingType;

    @Enumerated(EnumType.STRING)
    private DepartureType departureType;
    @Column(name = "departure_date")
    private LocalDate departureDate;
    @Column(columnDefinition = "TIME WITHOUT TIME ZONE")
    private LocalTime departureTime;

    private LocalDate arrivalDate;
    @Column(columnDefinition = "TIME WITHOUT TIME ZONE")
    private LocalTime arrivalTime;

    private int durationMinutes;
    private Integer price;
    private int availableSeats;
}
