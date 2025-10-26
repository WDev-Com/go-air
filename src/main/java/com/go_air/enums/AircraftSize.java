package com.go_air.enums;

public enum AircraftSize {
    LIGHT("Light", 15, 40),
    MEDIUM("Medium", 120, 200),
    LARGE("Large", 250, 450),
    JUMBO("Jumbo", 500, 750);

    private final String displayName;
    private final int minSeats;
    private final int maxSeats;

    AircraftSize(String displayName, int minSeats, int maxSeats) {
        this.displayName = displayName;
        this.minSeats = minSeats;
        this.maxSeats = maxSeats;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getMinSeats() {
        return minSeats;
    }

    public int getMaxSeats() {
        return maxSeats;
    }
}