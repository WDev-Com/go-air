package com.go_air.repo;

import com.go_air.entity.Passenger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PassengerRepository extends JpaRepository<Passenger, Long> {
	
	Passenger findByPassportNumber(String passportNumber);

	List<Passenger> findByUser_UserID(String userId);
}
