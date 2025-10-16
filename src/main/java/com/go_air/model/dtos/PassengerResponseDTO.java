package com.go_air.model.dtos;

import com.go_air.enums.Gender;

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
}
