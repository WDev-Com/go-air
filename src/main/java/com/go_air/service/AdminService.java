package com.go_air.service;

import com.go_air.entity.Flights;
import com.go_air.repo.FlightRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class AdminService {

    @Autowired
    private FlightRepository flightRepo;

    // Create flight
    public Flights createFlight(Flights flight) {
        return flightRepo.save(flight);
    }

    // Read all flights
    public List<Flights> getAllFlights() {
        return flightRepo.findAll();
    }

    // Get flight by flight number
    public Flights getFlightByFlightNumber(String flightNumber) {
        return flightRepo.findByFlightNumber(flightNumber).orElse(null);
    }

    // Update flight by flight number
    public Flights updateFlightByFlightNumber(String flightNumber, Flights updatedFlight) {
        Flights flight = flightRepo.findByFlightNumber(flightNumber).orElse(null);
        if (flight != null) {
            flight.setAirline(updatedFlight.getAirline());
            flight.setSourceAirport(updatedFlight.getSourceAirport());
            flight.setDestinationAirport(updatedFlight.getDestinationAirport());
            flight.setStop(updatedFlight.getStop());
            flight.setDestinationStop(updatedFlight.getDestinationStop());
            flight.setBookingType(updatedFlight.getBookingType());
            flight.setDepartureType(updatedFlight.getDepartureType());
            flight.setDepartureDate(updatedFlight.getDepartureDate());
            flight.setDepartureTime(updatedFlight.getDepartureTime());
            flight.setArrivalDate(updatedFlight.getArrivalDate());
            flight.setArrivalTime(updatedFlight.getArrivalTime());
            flight.setDurationMinutes(updatedFlight.getDurationMinutes());
            flight.setPrice(updatedFlight.getPrice());
            flight.setAvailableSeats(updatedFlight.getAvailableSeats());
            return flightRepo.save(flight);
        }
        return null;
    }

    // Delete flight by flight number
    public String deleteFlightByFlightNumber(String flightNumber) {
        Flights flight = flightRepo.findByFlightNumber(flightNumber).orElse(null);
        if (flight != null) {
            flightRepo.delete(flight);
            return "Flight with number " + flightNumber + " deleted successfully.";
        }
        return "Flight with number " + flightNumber + " not found.";
    }
}
