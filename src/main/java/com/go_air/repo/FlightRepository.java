package com.go_air.repo;

import com.go_air.entity.Flights;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface FlightRepository extends JpaRepository<Flights, Long> {

	Optional<Flights> findByFlightNumber(String flightNumber);

	@Query(value = """
			SELECT * FROM flights f
			WHERE
			    f.airline = COALESCE(:airline, f.airline) AND
			    f.source_airport = COALESCE(:sourceAirport, f.source_airport) AND
			    f.destination_airport = COALESCE(:destinationAirport, f.destination_airport) AND
			    f.departure_date = COALESCE(:departureDate, f.departure_date) AND
			    f.stop = COALESCE(:stop, f.stop) AND
			    f.booking_type = COALESCE(:bookingType, f.booking_type) AND
			    f.departure_type = COALESCE(:departureType, f.departure_type) AND
			    f.price >= COALESCE(:minPrice, f.price) AND
			    f.price <= COALESCE(:maxPrice, f.price) AND
			    f.available_seats >= COALESCE(:passengers, f.available_seats)
			""", nativeQuery = true)
	    List<Flights> findFlightsByFilters(
	            @Param("airline") String airline,
	            @Param("sourceAirport") String sourceAirport,
	            @Param("destinationAirport") String destinationAirport,
	            @Param("departureDate") LocalDate departureDate,
	            @Param("stop") Integer stop,
	            @Param("bookingType") String bookingType,
	            @Param("departureType") String departureType,
	            @Param("minPrice") Integer minPrice,
	            @Param("maxPrice") Integer maxPrice,
	            @Param("passengers") Integer passengers
	    );



	
    // prevent duplicate flights
    Optional<Flights> findByFlightNumberAndDepartureDateAndDepartureTime(
            String flightNumber,
            LocalDate departureDate,
            LocalTime departureTime
    );
    
    
 // Method 2: Custom JPQL (optional if you need extra filters)
    @Query("SELECT f FROM Flights f WHERE f.departureDate = :departureDate")
    List<Flights> findFlightsByDepartureDate(LocalDate departureDate);
    
    
}
