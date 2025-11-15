package com.go_air.entity;

import java.util.Collection;
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
@ToString(exclude = {"bookings", "passengers"})
@Entity
@Table(name = "users")
public class User implements UserDetails{
	
	private static final long serialVersionUID = 1L;	 
	@Id	
	@Column(length = 6) 	 
	private String userID;	 
	private String username;
	private String password;
    private String name;
    private String address;
    
    @Column(unique = true, nullable = true)
    private String contact;
    @Column(unique = true, nullable = true)
    private String email;
    
    private String role;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Passenger> passengers;
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Booking> bookings;
    
    @OneToOne(mappedBy = "user")
    @JsonIgnore
    private RefreshToken refreshToken;
    
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role));
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
