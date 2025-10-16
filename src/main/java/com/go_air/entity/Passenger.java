package com.go_air.entity;

import com.go_air.enums.Gender;
import com.go_air.enums.SeatPreference;
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
    name = "passenger"
)
public class Passenger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    private int age;

    @Enumerated(EnumType.STRING)
    private SeatPreference seatPreference;

    private String passportNumber;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
}
