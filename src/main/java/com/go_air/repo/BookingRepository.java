package com.go_air.repo;

import com.go_air.entity.Booking;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {


	@Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END " +
		       "FROM Booking b JOIN b.passengers p " +
		       "WHERE b.flightNumber = :flightNumber " +
		       "AND b.user.userID = :userId " +
		       "AND p.name = :passengerName")
		boolean existsByFlightNumberAndUser_UserIDAndPassengers_Name(
		        @Param("flightNumber") String flightNumber,
		        @Param("userId") String userId,
		        @Param("passengerName") String passengerName);

	 // Check if a passenger (by passport) has overlapping booking
    @Query(value = """
        SELECT CASE WHEN COUNT(*) > 0 THEN true ELSE false END
        FROM booking b
        JOIN passenger p ON b.id = p.booking_id
        WHERE p.passport_number = :passportNumber
          AND (
               (b.departure_date < :arrDate OR (b.departure_date = :arrDate AND b.departure_time < :arrTime))
           AND (:depDate < b.arrival_date OR (:depDate = b.arrival_date AND :depTime < b.arrival_time))
          )
    """, nativeQuery = true)
    boolean existsBookingConflictByPassport(
            @Param("passportNumber") String passportNumber,
            @Param("depDate") LocalDate depDate,
            @Param("depTime") LocalTime depTime,
            @Param("arrDate") LocalDate arrDate,
            @Param("arrTime") LocalTime arrTime
    );
	
    // Find all bookings by user ID
    List<Booking> findByUser_UserID(String userId);

    // Check if a user already booked a flight
    boolean existsByFlightNumberAndUser_UserID(String flightNumber, String userId);

    // Find all bookings containing a specific passenger
    List<Booking> findByPassengers_Id(Long passengerId);
}
