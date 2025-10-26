package com.go_air.entity;

import com.go_air.enums.DepartureType;
import com.go_air.enums.Gender;
import com.go_air.enums.SeatType;
import com.go_air.enums.TravelClass;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"user"})
@Entity
@Table(
    name = "passenger"
)
public class Passenger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    
    private String seatNo;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    private int age;

    @Enumerated(EnumType.STRING)
    private SeatType seatType;

    @Enumerated(EnumType.STRING)
    private TravelClass travelClass;
    
    @Enumerated(EnumType.STRING)
    private DepartureType departureType;
    
    private String passportNumber;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
}
