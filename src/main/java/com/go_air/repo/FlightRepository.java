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
		        (:airlines IS NULL OR f.airline IN (:airlines)) AND
		        f.source_airport = COALESCE(:sourceAirport, f.source_airport) AND
		        f.destination_airport = COALESCE(:destinationAirport, f.destination_airport) AND
		        f.departure_date = COALESCE(:departureDate, f.departure_date) AND
		        f.stop = COALESCE(:stop, f.stop) AND
		        f.booking_type = COALESCE(:bookingType, f.booking_type) AND
		        f.departure_type = COALESCE(:departureType, f.departure_type) AND
		        f.price >= COALESCE(:minPrice, f.price) AND
		        f.price <= COALESCE(:maxPrice, f.price) AND
		        f.available_seats >= COALESCE(:passengers, f.available_seats) AND
		        f.aircraft_size = COALESCE(:aircraftSize, f.aircraft_size)
		""", nativeQuery = true)
		List<Flights> findFlightsByFilters(
		    @Param("airlines") List<String> airlines,
		    @Param("sourceAirport") String sourceAirport,
		    @Param("destinationAirport") String destinationAirport,
		    @Param("departureDate") LocalDate departureDate,
		    @Param("stop") Integer stop,
		    @Param("bookingType") String bookingType,
		    @Param("departureType") String departureType,
		    @Param("minPrice") Integer minPrice,
		    @Param("maxPrice") Integer maxPrice,
		    @Param("passengers") Integer passengers,
		    @Param("aircraftSize") String aircraftSize
		);

   // For Admin
	@Query(value = """
		    SELECT * FROM flights f
		    WHERE
		        (COALESCE(:airlinesText, '') = '' OR f.airline = ANY(string_to_array(:airlinesText, ','))) AND
		        (COALESCE(:sourceAirport, '') = '' OR f.source_airport = :sourceAirport) AND
		        (COALESCE(:destinationAirport, '') = '' OR f.destination_airport = :destinationAirport) AND
		        (:departureDate IS NULL OR f.departure_date = CAST(:departureDate AS date)) AND
		        (:stop IS NULL OR f.stop = :stop) AND
		        (:bookingType IS NULL OR f.booking_type = CAST(:bookingType AS TEXT)) AND
		        (:departureType IS NULL OR f.departure_type = CAST(:departureType AS TEXT)) AND
		        (:minPrice IS NULL OR f.price >= :minPrice) AND
		        (:maxPrice IS NULL OR f.price <= :maxPrice) AND
		        (:aircraftSize IS NULL OR f.aircraft_size = CAST(:aircraftSize AS TEXT))
		    ORDER BY f.id
		""", nativeQuery = true)
		List<Flights> searchFlightsByPaginationAndFilters(
		        @Param("airlinesText") String airlinesText,
		        @Param("sourceAirport") String sourceAirport,
		        @Param("destinationAirport") String destinationAirport,
		        @Param("departureDate") String departureDate,
		        @Param("stop") Integer stop,
		        @Param("bookingType") String bookingType,
		        @Param("departureType") String departureType,
		        @Param("minPrice") Integer minPrice,
		        @Param("maxPrice") Integer maxPrice,
		        @Param("aircraftSize") String aircraftSize
		);

	
    // prevent duplicate flights
    Optional<Flights> findByFlightNumberAndDepartureDateAndDepartureTime(
            String flightNumber,
            LocalDate departureDate,
            LocalTime departureTime
    );
 
}
