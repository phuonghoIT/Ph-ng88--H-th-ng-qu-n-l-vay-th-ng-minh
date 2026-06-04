package com.example.demo.controller;

import com.example.demo.entity.LoanProduct;
import com.example.demo.service.LoanProductService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/loan-products")
@CrossOrigin(origins = "*")
public class LoanProductController {

    @Autowired
    private LoanProductService loanProductService;

    /**
     * 🟢 1. LẤY DANH SÁCH TẤT CẢ GÓI SẢN PHẨM VAY
     * GET http://localhost:8080/api/loan-products
     */
    @GetMapping
    public ResponseEntity<List<LoanProduct>> getAllLoanProducts() {
        return ResponseEntity.ok(loanProductService.getAllLoanProducts());
    }

    /**
     * 🟢 2. XEM CHI TIẾT MỘT GÓI SẢN PHẨM THEO ID
     * GET http://localhost:8080/api/loan-products/3
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getLoanProductById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(loanProductService.getLoanProductById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    /**
     * 🔵 3. TẠO MỚI GÓI SẢN PHẨM VAY (Admin)
     * POST http://localhost:8080/api/loan-products
     */
    @PostMapping
    public ResponseEntity<?> createLoanProduct(@Valid @RequestBody LoanProduct newProduct) {
        try {
            LoanProduct saved = loanProductService.createLoanProduct(newProduct);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    /**
     * 🟡 4. CẬP NHẬT GÓI SẢN PHẨM (Lãi suất, thời hạn...)
     * PUT http://localhost:8080/api/loan-products/3
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateLoanProduct(
            @PathVariable Long id,
            @Valid @RequestBody LoanProduct updatedData
    ) {
        try {
            return ResponseEntity.ok(loanProductService.updateLoanProduct(id, updatedData));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}
