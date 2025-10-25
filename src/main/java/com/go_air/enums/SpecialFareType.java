package com.go_air.enums;

public enum SpecialFareType {
    REGULAR(0, 0),                  // No discount, no constraint
    STUDENT(500, 0),
    ARMED_FORCES(700, 0),
    SENIOR_CITIZEN(400, 0),
    DOCTOR_AND_NURSES(600, 0),
    FAMILY(800, 2);                  // Discount 800, min 2 passengers

    private final Integer discount;
    private final int minPassengers;

    SpecialFareType(Integer discount, int minPassengers) {
        this.discount = discount;
        this.minPassengers = minPassengers;
    }

    public Integer applyDiscount(Integer maxPrice) {
        if (maxPrice == null) maxPrice = 10000;  // default maxPrice
        return maxPrice - discount;
    }

    public void validatePassengers(Integer passengers) {
        if (passengers == null) passengers = 1; // default 1
        if (passengers < minPassengers) {
            throw new RuntimeException(
                this.name() + " fare requires at least " + minPassengers + " passenger(s)."
            );
        }
    }
}
