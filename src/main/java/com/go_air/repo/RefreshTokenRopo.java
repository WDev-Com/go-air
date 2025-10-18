package com.go_air.repo;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.go_air.entity.RefreshToken;


@Repository
public interface RefreshTokenRopo extends JpaRepository<RefreshToken, Integer> {

	// custom method
	Optional<RefreshToken> findByRefreshToken(String refreshToken); 
}