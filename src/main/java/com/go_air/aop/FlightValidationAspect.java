package com.go_air.aop;

import java.time.LocalDate;
import java.time.LocalTime;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

import com.go_air.entity.Booking;
import com.go_air.entity.Flights;
import com.go_air.entity.Passenger;
import com.go_air.entity.User;

@Aspect
@Component
public class FlightValidationAspect {

    // Run for all Flights, Booking, and User objects in controller methods
    @Before("@annotation(com.go_air.aop.ValidateFlightData) || execution(* com.go_air.controller.UserController.*(..))")
    public void validateControllerInputs(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();

        for (Object arg : args) {
            if (arg instanceof Flights flight) {
                validateFlight(flight);
            } else if (arg instanceof Booking booking) {
                validateBooking(booking);
            } else if (arg instanceof User user) {
                validateUser(user);
            }
        }
    }

    // FLIGHT VALIDATION 
    private void validateFlight(Flights flight) {
        if (isBlank(flight.getAirline()))
            throw new IllegalArgumentException("Airline name is required");

        if (isBlank(flight.getFlightNumber()))
            throw new IllegalArgumentException("Flight number is required");

        if (isBlank(flight.getSourceAirport()))
            throw new IllegalArgumentException("Source airport is required");

        if (isBlank(flight.getDestinationAirport()))
            throw new IllegalArgumentException("Destination airport is required");

        if (flight.getBookingType() == null)
            throw new IllegalArgumentException("Booking type is required");

        if (flight.getDepartureType() == null)
            throw new IllegalArgumentException("Departure type is required");

        if (flight.getDepartureDate() == null)
            throw new IllegalArgumentException("Departure date is required");

        if (flight.getDepartureTime() == null)
            throw new IllegalArgumentException("Departure time is required");

        if (flight.getArrivalDate() == null)
            throw new IllegalArgumentException("Arrival date is required");

        if (flight.getArrivalTime() == null)
            throw new IllegalArgumentException("Arrival time is required");

        if (flight.getSourceAirport().equalsIgnoreCase(flight.getDestinationAirport()))
            throw new IllegalArgumentException("Source and destination airports cannot be the same");

        LocalDate depDate = flight.getDepartureDate();
        LocalDate arrDate = flight.getArrivalDate();
        LocalTime depTime = flight.getDepartureTime();
        LocalTime arrTime = flight.getArrivalTime();

        if (arrDate.isBefore(depDate) ||
            (arrDate.isEqual(depDate) && arrTime.isBefore(depTime)))
            throw new IllegalArgumentException("Arrival must be after departure");

        if (flight.getDurationMinutes() <= 0)
            throw new IllegalArgumentException("Flight duration must be positive");

        if (flight.getPrice() <= 0)
            throw new IllegalArgumentException("Price must be positive");

        if (flight.getAvailableSeats() < 0)
            throw new IllegalArgumentException("Available seats cannot be negative");

        if (flight.getStop() < 0)
            throw new IllegalArgumentException("Number of stops cannot be negative");
    }

    private boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }

    // BOOKING VALIDATION
    private void validateBooking(Booking booking) {
        if (booking.getFlightNumber() == null || booking.getFlightNumber().isBlank())
            throw new IllegalArgumentException("Flight number is required");

        if (booking.getTripType() == null)
            throw new IllegalArgumentException("Trip type must be specified");

        if (booking.getPassengers() == null || booking.getPassengers().isEmpty())
            throw new IllegalArgumentException("At least one passenger is required");

        for (Passenger p : booking.getPassengers()) {
            if (p.getName() == null || p.getName().isBlank())
                throw new IllegalArgumentException("Passenger name is required");

            if (p.getGender() == null)
                throw new IllegalArgumentException("Passenger gender is required");

            if (p.getAge() <= 0)
                throw new IllegalArgumentException("Passenger age must be greater than 0");
        }
    }

    // USER VALIDATION 
    private void validateUser(User user) {
    	// Name validation
    	if (isBlank(user.getName()))
            throw new IllegalArgumentException("User name is required");

        // Email validation using regex
        if (isBlank(user.getEmail()))
            throw new IllegalArgumentException("Email is required");

        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
        if (!user.getEmail().matches(emailRegex))
            throw new IllegalArgumentException("Invalid email format");

        // Contact validation: not blank and digits only
        if (isBlank(user.getContact()))
            throw new IllegalArgumentException("Contact number is required");

        String contactRegex = "^[7-9][0-9]{9}$"; // starts with 7/8/9 and total 10 digits
        if (!user.getContact().matches(contactRegex))
            throw new IllegalArgumentException("Invalid contact number format. Must be 10 digits starting with 7, 8, or 9");


    }
}
