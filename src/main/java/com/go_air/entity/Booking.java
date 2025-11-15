package com.go_air.entity;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.go_air.enums.AircraftSize;
import com.go_air.enums.BookingStatus;
import com.go_air.enums.JourneyStatus;
import com.go_air.enums.PaymentStatus;
import com.go_air.enums.SpecialFareType;
import com.go_air.enums.TripType;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"user", "passengers"})
@Entity
@Table(
    name = "booking"
)
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String bookingNo;

    private String flightNumber;

    @Enumerated(EnumType.STRING)
    private AircraftSize aircraftSize;
    
    @Enumerated(EnumType.STRING)
    private TripType tripType;

    private LocalTime departureTime; 
    private LocalDate departureDate;
    
    private LocalTime arrivalTime; 
    private LocalDate arrivalDate;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    private String contactEmail;
    
    private String contactPhone;

    private LocalDateTime bookingTime;
    
    private int passengerCount;
    
    private double totalAmount;

    @Enumerated(EnumType.STRING)
    private BookingStatus status;
    
    @Enumerated(EnumType.STRING)
    private SpecialFareType specialFareType;
    
    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;
    
    private String paymentID;
    
    
    @Enumerated(EnumType.STRING)
    private JourneyStatus journeyStatus;
    
   // One booking can have multiple passengers
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "booking_id") // foreign key in Passenger table
    @JsonBackReference("booking-passenger")
    private List<Passenger> passengers;
}