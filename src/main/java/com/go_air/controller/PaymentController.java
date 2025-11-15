package com.go_air.controller;

import com.go_air.entity.Booking;
import com.go_air.enums.BookingStatus;
import com.go_air.enums.PaymentStatus;
import com.go_air.service.UserService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

@RestController
@RequestMapping("/api/payment")
@CrossOrigin(origins = "http://localhost:5173")
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);
 
    @Autowired
    private UserService userService;
    
    
    
    @PostMapping("/create-checkout-session/{userId}")
    public ResponseEntity<Map<String, Object>> createCheckoutSession(
            @PathVariable String userId,
            @RequestBody List<Booking> bookingRequests) throws StripeException {

    	 if (bookingRequests == null || bookingRequests.isEmpty()) {
    	        return ResponseEntity.badRequest().body(Map.of("error", "No bookings provided"));
    	    }

    	    // Generate ONE common booking number for all flights in this booking
    	    String bookingNo = userService.generateUniqueBookingNumber();

    	    long totalAmount = 0L;
    	    StringBuilder productNames = new StringBuilder();

    	    // Save all bookings with SAME bookingNo
    	    for (Booking bookingRequest : bookingRequests) {

    	        // FIX: Assign bookingNo BEFORE saving
    	        bookingRequest.setBookingNo(bookingNo);

    	        // Save to DB
    	        userService.bookFlight(userId, bookingRequest);

    	        // Calculate total
    	        totalAmount += bookingRequest.getTotalAmount();

    	        if (productNames.length() > 0) productNames.append(", ");
    	        productNames.append(bookingRequest.getFlightNumber());
    	    }

    	    long amountInPaise = totalAmount * 100;

        // 3️ Build Stripe session
        SessionCreateParams params = SessionCreateParams.builder()
                .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl("http://localhost:5173/success?session_id={CHECKOUT_SESSION_ID}&userId=" + userId+"&bookingNo="+bookingNo)
                .setCancelUrl("http://localhost:5173/cancel")
                .putMetadata("flights", productNames.toString())
                .setCustomerEmail(bookingRequests.get(0).getContactEmail())
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setQuantity(1L)
                                .setPriceData(
                                        SessionCreateParams.LineItem.PriceData.builder()
                                                .setCurrency("inr")
                                                .setUnitAmount(amountInPaise)
                                                .setProductData(
                                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                .setName("Flights: " + productNames)
                                                                .build()
                                                )
                                                .build()
                                )
                                .build()
                )
                .build();

        Session session = Session.create(params);

        Map<String, Object> response = new HashMap<>();
        response.put("url", session.getUrl());
        response.put("session_id", session.getId());
        response.put("payment_intent", session.getPaymentIntent());

        return ResponseEntity.ok(response);
    }


    @GetMapping("/verify-payment")
    public ResponseEntity<?> verifyPayment(
            @RequestParam String session_id,
            @RequestParam String userId,
            @RequestParam String bookingNo) {
    	 
    	   
        try {
            log.info("userId = {}", userId);

            // Retrieve Stripe Checkout Session
            Session session = Session.retrieve(session_id);
            String paymentIntentId = session.getPaymentIntent();

            // Retrieve PaymentIntent
            PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);
            String stripeStatus = paymentIntent.getStatus();

            // Fetch updated bookings from DB
            List<Booking> userBookings = userService.getBookingsByBookingNo(bookingNo);

            String bookingNumber = userBookings.stream()
                    .map(Booking::getBookingNo)
                    .findFirst()
                    .orElse(null);


            Map<String, Object> data = new HashMap<>();
            data.put("payment_id", paymentIntentId);
            data.put("payment_status", stripeStatus);
            data.put("amount", session.getAmountTotal());
            data.put("currency", session.getCurrency());
            data.put("customer_email", session.getCustomerEmail());
            data.put("productName", session.getMetadata().getOrDefault("product_name", "Test Product"));

            // Add booking details
            data.put("userId", userId);
            data.put("bookingNumber", bookingNumber);
            data.put("bookingDetails", userBookings);
            data.put("bookingTime", LocalDateTime.now());

            log.info("Stripe Payment Status => {}", stripeStatus);

            // CASE 1: Payment Succeeded
            if ("succeeded".equals(stripeStatus)) {

                userService.updateBookingAfterPayment(
                        userId,
                        BookingStatus.CONFIRMED,
                        PaymentStatus.SUCCESS,
                        paymentIntentId
                );

                data.put("booking_status", "CONFIRMED");
                data.put("message", "Flights booked successfully!");

                return ResponseEntity.ok(Map.of(
                        "status", "success",
                        "response", data
                ));
            }

            // CASE 2: Payment Failed
            userService.updateBookingAfterPayment(
                    userId,
                    BookingStatus.PENDING,
                    PaymentStatus.FAILED,
                    paymentIntentId
            );

            data.put("booking_status", "PENDING");
            data.put("message", "Payment failed! Booking kept in PENDING state.");

            return ResponseEntity.ok(Map.of(
                    "status", stripeStatus,
                    "response", data
            ));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

   
    // For Test Purpose Handle Stripe Webhooks
    @PostMapping("/webhook")
    public ResponseEntity<String> handleStripeWebhook(HttpServletRequest request) {
        String payload = "";
        try (Scanner s = new Scanner(request.getInputStream(), StandardCharsets.UTF_8.name())) {
            payload = s.useDelimiter("\\A").hasNext() ? s.next() : "";
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Failed to read payload");
        }

        String sigHeader = request.getHeader("Stripe-Signature");
        // Change this when restart the stripe CLI
        String endpointSecret = "whsec_fd0cad1feb5f96b473619f6d2334108598643a89300111b312092f0de5ea2aa5";

        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, endpointSecret);
        } catch (SignatureVerificationException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
        }

        // Handle the event
        if ("checkout.session.completed".equals(event.getType())) {
            Session session = (Session) event.getDataObjectDeserializer()
                    .getObject()
                    .orElse(null);

            if (session != null) {
                String paymentIntentId = session.getPaymentIntent(); // ✅ here it’s not null
                String customerEmail = session.getCustomerEmail();
                String productName = session.getMetadata().getOrDefault("product_name", "Test Product");
                Long amountTotal = session.getAmountTotal();

                log.info("✅ Payment success:");
                log.info("PaymentIntent ID: " + paymentIntentId);
                log.info("Customer Email: " + customerEmail);
                log.info("Amount: " + amountTotal);
                log.info("Product: " + productName);

            
            }
        }

        

        return ResponseEntity.ok("Webhook handled");
    }


    // Simple Test Routes
    @GetMapping("/success")
    public String success() {
        return "payment successful";
    }

    @GetMapping("/cancel")
    public String cancel() {
        return "payment cancelled";
    }
}
