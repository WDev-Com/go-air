package com.go_air.entity;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import com.go_air.enums.BookingStatus;
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

    private String flightNumber;

    @Enumerated(EnumType.STRING)
    private TripType tripType;

    private LocalTime departureTime; 
    private LocalDate departureDate;
    
    private LocalTime arrivalTime; 
    private LocalDate arrivalDate;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String contactEmail;
    private String contactPhone;

    private LocalDateTime bookingTime;
    private int passengerCount;
    private double totalAmount;

    @Enumerated(EnumType.STRING)
    private BookingStatus status;
 
   // One booking can have multiple passengers
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "booking_id") // foreign key in Passenger table
    private List<Passenger> passengers;
}