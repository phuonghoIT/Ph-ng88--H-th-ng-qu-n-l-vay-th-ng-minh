package com.example.demo.controller;

import com.example.demo.entity.Payment;
import com.example.demo.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = "*")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    /**
     * 🟢 1. LẤY DANH SÁCH TẤT CẢ GIAO DỊCH THANH TOÁN
     * GET http://localhost:8080/api/payments
     */
    @GetMapping
    public ResponseEntity<List<Payment>> getAllPayments() {
        return ResponseEntity.ok(paymentService.getAllPayments());
    }

    /**
     * 🟢 2. XEM CHI TIẾT MỘT GIAO DỊCH THEO ID
     * GET http://localhost:8080/api/payments/8
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getPaymentById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(paymentService.getPaymentById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    /**
     * 🔵 3. GHI NHẬN THANH TOÁN (Tự động cập nhật trạng thái kỳ nợ nếu đủ tiền)
     * POST http://localhost:8080/api/payments
     */
    @PostMapping
    public ResponseEntity<?> processPayment(@Valid @RequestBody Payment newPayment) {
        try {
            Payment saved = paymentService.processPayment(newPayment);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}
