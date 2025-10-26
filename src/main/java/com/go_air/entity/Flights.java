package com.go_air.entity;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import com.go_air.enums.AircraftSize;
import com.go_air.enums.BookingType;
import com.go_air.enums.DepartureType;
import com.go_air.enums.JourneyStatus;

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
    
    @Enumerated(EnumType.STRING)
    private AircraftSize aircraftSize;
    
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
    
    @Enumerated(EnumType.STRING)
    @Column(name = "journey_status", nullable = false, columnDefinition = "varchar(255) default 'SCHEDULED'")
    @Builder.Default
    private JourneyStatus journeyStatus = JourneyStatus.SCHEDULED;


 
    @OneToMany(mappedBy = "flight", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Seat> seats;
}


