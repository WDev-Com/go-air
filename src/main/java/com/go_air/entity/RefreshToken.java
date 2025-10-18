package com.go_air.entity;

import java.time.Instant;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "refersh_token")
@Data
@Getter
@Setter
@AllArgsConstructor
@RequiredArgsConstructor
@Builder
public class RefreshToken {
  
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private int tokenID;
	
	private String refreshToken;
	
	private Instant expiry;
	
	@OneToOne
	private User user;
	
	@Override
	public String toString() {
	    return "RefreshToken{" +
	            "refreshToken='" + refreshToken + '\'' +
	            ", expiry=" + expiry +
	            ", user=" + (user != null ? user.getUsername() : "null") + // or some other representation
	            '}';
	}

}