package com.go_air.repo;

import com.go_air.entity.Flights;
import com.go_air.enums.BookingType;
import com.go_air.enums.DepartureType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface FlightRepository extends JpaRepository<Flights, Long> {

	Optional<Flights> findByFlightNumber(String flightNumber);


	@Query("SELECT f FROM Flights f WHERE " +
	           "(:airline IS NULL OR LOWER(f.airline) = LOWER(:airline)) AND " +
	           "(:sourceAirport IS NULL OR LOWER(f.sourceAirport) = LOWER(:sourceAirport)) AND " +
	           "(:destinationAirport IS NULL OR LOWER(f.destinationAirport) = LOWER(:destinationAirport)) AND " +
	           "(:stop IS NULL OR f.stop = :stop) AND " +
	           "(:bookingType IS NULL OR f.bookingType = :bookingType) AND " +
	           "(:departureType IS NULL OR f.departureType = :departureType) AND " +
	           "(:minPrice IS NULL OR f.price >= :minPrice) AND " +
	           "(:maxPrice IS NULL OR f.price <= :maxPrice) AND " +
	           "(:passengers IS NULL OR f.availableSeats >= :passengers)")
	    List<Flights> findFlightsByFilters(
	            String airline,
	            String sourceAirport,
	            String destinationAirport,
	            Integer stop,
	            BookingType bookingType,
	            DepartureType departureType,
	            Double minPrice,
	            Double maxPrice,
	            Integer passengers
	    );
	
    // prevent duplicate flights
    Optional<Flights> findByFlightNumberAndDepartureDateAndDepartureTime(
            String flightNumber,
            LocalDate departureDate,
            LocalTime departureTime
    );
}
