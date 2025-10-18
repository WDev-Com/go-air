package com.go_air.repo;

import com.go_air.entity.User;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

    User findByEmail(String email);
    
    User findByUsername(String username);
    
    // Find user by userID
    Optional<User> findByUserID(String userID);

    // Check if a user exists by userID
    boolean existsByUserID(String userID);

    // Delete user by userID
    void deleteByUserID(String userID);

    // Optional: find users by name (search)
    List<User> findByNameContainingIgnoreCase(String name);
}
