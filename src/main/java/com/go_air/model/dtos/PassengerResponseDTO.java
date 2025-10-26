package com.go_air.model.dtos;

import com.go_air.enums.DepartureType;
import com.go_air.enums.Gender;
import com.go_air.enums.SeatType;
import com.go_air.enums.TravelClass;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PassengerResponseDTO {

    private Long passengerId;
    private String name;
    
    @Enumerated(EnumType.STRING)
    private Gender gender;
    
    private int age;
    private String passportNumber;
    private String seatNo;
    
    @Enumerated(EnumType.STRING)
    private SeatType seatType;
    
    @Enumerated(EnumType.STRING)
    private TravelClass travelClass;
    
    @Enumerated(EnumType.STRING)
    private DepartureType departureType;
}
