package com.go_air.repo;

import com.go_air.entity.Seat;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SeatRepository extends JpaRepository<Seat, Long> {
	
	 @Query("SELECT s FROM Seat s WHERE s.flight.flightNumber = :flightNumber ORDER BY s.rowNumber, s.columnLabel")
	    List<Seat> findSeatsByFlightNumber(@Param("flightNumber") String flightNumber);

	 
}
