package com.go_air.entity;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import jakarta.persistence.*;
import lombok.*;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import com.fasterxml.jackson.annotation.JsonIgnore;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@Entity
@Table(name = "users")
public class User implements UserDetails{

	 @Id
	 @Column(length = 6) 
	 private String userID;
	 private String username;
	 private String password;
    private String name;
    private String address;
    @Column(unique = true, nullable = false)
    private String contact;
    @Column(unique = true, nullable = false)
    private String email;
    private String role;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    private List<Passenger> passengers;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    private List<Booking> bookings;
    
    @OneToOne(mappedBy = "user")
    @JsonIgnore
    private RefreshToken refreshToken;
    
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Assuming your role is a string, prefix it with "ROLE_" as it's a common practice
        String roleWithPrefix = "ROLE_" + role;
        return Collections.singletonList(new SimpleGrantedAuthority(roleWithPrefix));
    }
 @Override
 public String getPassword() {
 	// TODO Auto-generated method stub
 	return password;
 }
 @Override
 public String getUsername() {
 	// TODO Auto-generated method stub
 	return username;
 }
 @Override
 public boolean isAccountNonExpired() {
 	// TODO Auto-generated method stub
 	return true;
 }
 @Override
 public boolean isAccountNonLocked() {
 	// TODO Auto-generated method stub
 	return true;
 }
 @Override
 public boolean isCredentialsNonExpired() {
 	// TODO Auto-generated method stub
 	return true;
 }
 @Override
 public boolean isEnabled() {
 	// TODO Auto-generated method stub
 	return true;
 }


}
