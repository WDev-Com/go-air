package com.go_air.controller;

import java.security.Principal;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.go_air.aop.ValidateFlightData;
import com.go_air.entity.RefreshToken;
import com.go_air.entity.User;
import com.go_air.jwt.JWTHepler;
import com.go_air.model.dtos.JwtRequest;
import com.go_air.model.dtos.JwtResponse;
import com.go_air.model.dtos.RefreshTokenRequest;
import com.go_air.service.RefreshTokenService;
import com.go_air.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/auth")
public class AuthController{

	@Autowired
	private UserService userservice;
	 
	@Autowired 
	private RefreshTokenService refreshTokenService;
	
	
	@GetMapping("/user")
	public ResponseEntity<List<User>> getBook() {
		List<User> res = userservice.getAllUsers();
		if(res.size() <= 0) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
		}
		return  ResponseEntity.status(HttpStatus.CREATED).body(res);
	}
	
	@PreAuthorize("hasRole('ADMIN')")
	@GetMapping("/userprivate")
	public ResponseEntity<List<User>> getBookPrivate() {
	    List<User> res = userservice.getAllUsers();
	    if (res.size() <= 0) {
	        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
	    }
	    return ResponseEntity.status(HttpStatus.CREATED).body(res);
	}

	
	@GetMapping("/userbyusername/{username}")
	public ResponseEntity<User> getUserByusername(@PathVariable String username){
		User userModel = userservice.findByUserName(username);
		System.out.println(userModel);
		return ResponseEntity.status(HttpStatus.OK).body(userModel);
	}
	
	@GetMapping("/currentuser")
	public String getLoggedUer(Principal principal){		
		return principal.getName();
	}
	
	@PreAuthorize("hasAuthority('ADMIN')")
	@PostMapping("/adduser")
	public ResponseEntity<User> addUser(@RequestBody User userModel){
		User u = null;

			u = this.userservice.createUser(userModel);
		    System.out.println(u);
		    return ResponseEntity.status(HttpStatus.CREATED).body(u);
	}
	
	 // Create User (only username and password)
    @PostMapping("/signup")
    @ValidateFlightData
    public ResponseEntity<Map<String, String>> createUser(@RequestBody User userModel) {
        if (userModel.getUsername() == null || userModel.getPassword() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Username and Password are required"));
        }

        User createdUser = userservice.createUser(userModel);
        return ResponseEntity.status(HttpStatus.OK)
                .body(Map.of("message", "User created successfully", "userID", createdUser.getUserID()));
    }
    
    // Check Username Availability
    @GetMapping("/checkusername")
    public ResponseEntity<Map<String, String>> checkUsernameAvailability(@RequestParam String username) {
        boolean exists = userservice.existsByUsername(username);
        if (exists) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Username is already taken"));
        }
        return ResponseEntity.ok(Map.of("message", "Username is available"));
    }

   
	@ExceptionHandler(DataIntegrityViolationException.class)
    public String exceptionHandlerDupicateUsername(String str) {
        return  str;
    }

	// ID is important in JPA configuration for updating the data
	@PatchMapping("/updateuser/{username}")
	@ValidateFlightData
	public ResponseEntity<String> updateUser(@PathVariable String username, @RequestBody User userModel) {
	    userservice.updateUserByUsername(username, userModel);
	    System.out.println(userModel);
	    return ResponseEntity.status(HttpStatus.OK).body("Updated Successfully");
	}

	@DeleteMapping("/delete/{username}")
	public ResponseEntity<String> deleteUser(@PathVariable String username){
		userservice.deleteUser(username);
		System.out.println("Deleted : "+username);
		return ResponseEntity.status(HttpStatus.OK).build();
		
	}
	
	///////////////////////////////////// JWT AUth
	
	  @Autowired
	    private UserDetailsService userDetailsService;

	    @Autowired
	    private AuthenticationManager manager;


	    @Autowired
	    private JWTHepler helper;

	    private Logger logger = LoggerFactory.getLogger(AuthController.class);


	    @PostMapping("/login")
	    public ResponseEntity<JwtResponse> login(@RequestBody JwtRequest request) {
	    	 System.out.println("IN CONTROLLER :- "+request.getUsername().toString() + " : "+ request.getPassword());
	        this.doAuthenticate(request.getUsername(), request.getPassword());

	        
	        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getUsername());
	        logger.atInfo().log("Line 168 AuthControler | Loaded User : ",userDetails);
	        //String pass = userDetails.getPassword()
	        User user = (User) userDetails;
	        String token = this.helper.generateToken(userDetails);
            RefreshToken refreshToken =  this.refreshTokenService.createRefreshToken(userDetails.getUsername());
	        JwtResponse response = JwtResponse.builder()
	                .jwtToken(token)
	                .role(user.getRole())
	                .refreshToken(refreshToken.getRefreshToken())
	                .username(userDetails.getUsername()).build();
	        return new ResponseEntity<>(response, HttpStatus.OK);
	    }
	    
	    
	    @PostMapping("/logout")
	    public ResponseEntity<String> logout(HttpServletRequest request, HttpServletResponse response) {
	        // You can perform additional actions upon logout if needed
	        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
	        if (authentication != null) {
	            new SecurityContextLogoutHandler().logout(request, response, authentication);
	        }

	        return ResponseEntity.ok("Logout successful");
	    }

	    @PostMapping("/refreshToken")
	    public JwtResponse refreshJwtToken(@RequestBody RefreshTokenRequest request){
	    	RefreshToken refreshTokenOBJ = refreshTokenService.verifyRefreshToken(request.getRefreshToken());
	    	User userModel =  refreshTokenOBJ.getUser();
	    	String token = this.helper.generateToken(userModel);
	    	return JwtResponse.builder()
	    			.username(userModel.getUsername())
	    			.jwtToken(token)
	    			.refreshToken(refreshTokenOBJ.getRefreshToken())
	    			.build();
	    }
	    
	    //////////////////////////////////// Credentials Invalid !!
	    @ExceptionHandler(BadCredentialsException.class)
	    public String exceptionHandler(String msg) {
	        return msg;
	    }
	    private void doAuthenticate(String username, String password) {

	        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(username, password);
	            manager.authenticate(authentication);
	    }
       
	
}