package com.go_air.jwt;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JWTSecureEntryPoint implements AuthenticationEntryPoint{

	@Override
	public void commence(HttpServletRequest request,
	                     HttpServletResponse response,
	                     AuthenticationException authException) throws IOException {

	    response.setContentType("application/json");
	    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

	    Map<String, String> resp = new HashMap<>();
	    resp.put("status", "FAILED");
	    resp.put("message", "JWT Authentication Failed: " + authException.getMessage());

	    response.getWriter().write(new ObjectMapper().writeValueAsString(resp));
	}


}