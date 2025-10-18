package com.go_air.model.dtos;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class JwtResponse {
  
	private String username;
	private String jwtToken;
	private String refreshToken;
}