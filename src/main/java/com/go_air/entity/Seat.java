package com.go_air.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.go_air.enums.SeatPosition;
import com.go_air.enums.SeatStatus;
import com.go_air.enums.SeatType;
import com.go_air.enums.TravelClass;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "flight")
@Entity
@Table(name = "seats")
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String seatNumber;

    private int rowNumber;
    
    private String columnLabel; 

    @Enumerated(EnumType.STRING)
    private SeatType seatType; 

    @Enumerated(EnumType.STRING)
    private SeatPosition seatPosition; 

    @Enumerated(EnumType.STRING)
    private SeatStatus seatStatus;

    @Enumerated(EnumType.STRING)
    private TravelClass travelClass; 

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flight_number",referencedColumnName = "flightNumber", nullable = false)
    @JsonIgnore
    private Flights flight;
}
