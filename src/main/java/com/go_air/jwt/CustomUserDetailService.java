package com.go_air.jwt;


import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.go_air.entity.User;
import com.go_air.service.UserService;



@Service
public class CustomUserDetailService implements UserDetailsService {
	
	@Autowired
	private UserService userservice;

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		// TODO Auto-generated method stub
		User user = userservice.findByUserName(username);
		System.out.println("At CustomUserDetail "+user);
		  if (user == null) {
	            throw new UsernameNotFoundException("User not found with username: " + username);
	        }
	        return user;
	    }
	}