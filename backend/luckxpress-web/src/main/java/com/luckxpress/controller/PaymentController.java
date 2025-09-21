package com.luckxpress.controller;

import com.luckxpress.data.entity.Payment;
import com.luckxpress.data.entity.LuckUser;
import com.luckxpress.data.repository.PaymentRepository;
import com.luckxpress.data.repository.LuckUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/v1/payments")
@CrossOrigin(origins = "http://localhost:3000")
public class PaymentController {

    @Autowired
    private PaymentRepository paymentRepository;
    
    @Autowired
    private LuckUserRepository userRepository;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getPayments(@RequestParam(defaultValue = "0") int page,
                                                          @RequestParam(defaultValue = "10") int size,
                                                          @RequestParam(defaultValue = "id") String sort,
                                                          @RequestParam(defaultValue = "desc") String order,
                                                          @RequestParam(required = false) Long userId,
                                                          @RequestParam(required = false) String type,
                                                          @RequestParam(required = false) String status) {
        
        Sort.Direction direction = order.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sort));
        
        Payment.PaymentType typeEnum = null;
        if (type != null && !type.isEmpty()) {
            try {
                typeEnum = Payment.PaymentType.valueOf(type.toUpperCase());
            } catch (IllegalArgumentException e) {
                // Invalid type, ignore filter
            }
        }
        
        Payment.PaymentStatus statusEnum = null;
        if (status != null && !status.isEmpty()) {
            try {
                statusEnum = Payment.PaymentStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                // Invalid status, ignore filter
            }
        }
        
        Page<Payment> payments = paymentRepository.findPaymentsWithFilters(userId, typeEnum, statusEnum, null, null, pageable);
        
        Map<String, Object> response = new HashMap<>();
        response.put("data", payments.getContent());
        response.put("total", payments.getTotalElements());
        response.put("page", page);
        response.put("size", size);
        response.put("totalPages", payments.getTotalPages());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getPayment(@PathVariable Long id) {
        Optional<Payment> payment = paymentRepository.findById(id);
        if (payment.isPresent()) {
            Map<String, Object> response = new HashMap<>();
            response.put("data", payment.get());
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createPayment(@RequestBody Map<String, Object> paymentData) {
        try {
            Long userId = Long.valueOf(paymentData.get("userId").toString());
            Optional<LuckUser> user = userRepository.findById(userId);
            
            if (!user.isPresent()) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "User not found");
                return ResponseEntity.badRequest().body(error);
            }
            
            Payment payment = new Payment();
            payment.setUser(user.get());
            payment.setType(Payment.PaymentType.valueOf(((String) paymentData.get("type")).toUpperCase()));
            payment.setAmount(new BigDecimal(paymentData.get("amount").toString()));
            payment.setCurrency((String) paymentData.getOrDefault("currency", "USD"));
            payment.setPaymentMethod((String) paymentData.get("paymentMethod"));
            payment.setProvider((String) paymentData.get("provider"));
            payment.setNotes((String) paymentData.get("notes"));
            
            if (paymentData.containsKey("fee")) {
                payment.setFee(new BigDecimal(paymentData.get("fee").toString()));
                payment.setNetAmount(payment.getAmount().subtract(payment.getFee()));
            }
            
            Payment savedPayment = paymentRepository.save(payment);
            
            Map<String, Object> response = new HashMap<>();
            response.put("data", savedPayment);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to create payment: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updatePayment(@PathVariable Long id, @RequestBody Map<String, Object> paymentData) {
        Optional<Payment> existingPayment = paymentRepository.findById(id);
        if (!existingPayment.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        
        try {
            Payment payment = existingPayment.get();
            
            if (paymentData.containsKey("status")) {
                Payment.PaymentStatus newStatus = Payment.PaymentStatus.valueOf(((String) paymentData.get("status")).toUpperCase());
                payment.setStatus(newStatus);
                
                if (newStatus == Payment.PaymentStatus.COMPLETED) {
                    payment.markAsProcessed();
                } else if (newStatus == Payment.PaymentStatus.FAILED) {
                    payment.markAsFailed((String) paymentData.get("failureReason"));
                }
            }
            
            if (paymentData.containsKey("notes")) {
                payment.setNotes((String) paymentData.get("notes"));
            }
            
            if (paymentData.containsKey("providerTransactionId")) {
                payment.setProviderTransactionId((String) paymentData.get("providerTransactionId"));
            }
            
            Payment savedPayment = paymentRepository.save(payment);
            
            Map<String, Object> response = new HashMap<>();
            response.put("data", savedPayment);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to update payment: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deletePayment(@PathVariable Long id) {
        if (!paymentRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        
        paymentRepository.deleteById(id);
        
        Map<String, Object> response = new HashMap<>();
        response.put("data", Map.of("id", id));
        return ResponseEntity.ok(response);
    }

    // Initialize sample payment data
    @PostMapping("/init-sample-data")
    public ResponseEntity<Map<String, Object>> initSamplePaymentData() {
        List<LuckUser> users = userRepository.findAll();
        if (users.isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "No users found. Please create users first.");
            return ResponseEntity.badRequest().body(error);
        }

        List<Payment> samplePayments = new ArrayList<>();
        
        // Create sample payments for existing users
        for (int i = 0; i < Math.min(users.size(), 10); i++) {
            LuckUser user = users.get(i % users.size());
            
            // Deposit
            Payment deposit = new Payment(user, Payment.PaymentType.DEPOSIT, new BigDecimal("100.00"), "USD");
            deposit.setPaymentMethod("Credit Card");
            deposit.setProvider("Stripe");
            deposit.setStatus(Payment.PaymentStatus.COMPLETED);
            deposit.markAsProcessed();
            samplePayments.add(deposit);
            
            // Withdrawal
            if (i % 3 == 0) {
                Payment withdrawal = new Payment(user, Payment.PaymentType.WITHDRAWAL, new BigDecimal("50.00"), "USD");
                withdrawal.setPaymentMethod("Bank Transfer");
                withdrawal.setProvider("PayPal");
                withdrawal.setStatus(i % 2 == 0 ? Payment.PaymentStatus.PENDING : Payment.PaymentStatus.COMPLETED);
                if (withdrawal.getStatus() == Payment.PaymentStatus.COMPLETED) {
                    withdrawal.markAsProcessed();
                }
                samplePayments.add(withdrawal);
            }
        }

        paymentRepository.saveAll(samplePayments);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Sample payment data created successfully");
        response.put("count", samplePayments.size());
        return ResponseEntity.ok(response);
    }
}
