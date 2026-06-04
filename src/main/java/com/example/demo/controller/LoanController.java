package com.example.demo.controller;

import com.example.demo.entity.Loan;
import com.example.demo.entity.LoanStatus;
import com.example.demo.service.LoanService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/loans")
@CrossOrigin(origins = "*")
public class LoanController {

    @Autowired
    private LoanService loanService;

    /**
     * 🟢 1 & 2. LẤY DANH SÁCH KHOẢN VAY (có thể lọc theo khách hàng)
     * GET http://localhost:8080/api/loans
     * GET http://localhost:8080/api/loans?customerId=5
     */
    @GetMapping
    public ResponseEntity<List<Loan>> getLoans(@RequestParam(required = false) Long customerId) {
        if (customerId != null) {
            return ResponseEntity.ok(loanService.getLoansByCustomerId(customerId));
        }
        return ResponseEntity.ok(loanService.getAllLoans());
    }

    /**
     * 🟢 3. XEM CHI TIẾT MỘT KHOẢN VAY THEO ID
     * GET http://localhost:8080/api/loans/10
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getLoanById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(loanService.getLoanById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    /**
     * 🔵 4. TẠO KHOẢN VAY MỚI VÀ TỰ ĐỘNG SINH LỊCH TRẢ NỢ
     * POST http://localhost:8080/api/loans
     */
    @PostMapping
    public ResponseEntity<?> createLoan(@Valid @RequestBody Loan newLoan) {
        try {
            Loan saved = loanService.createLoanAndGenerateSchedule(newLoan);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    /**
     * 🟡 5. CẬP NHẬT TRẠNG THÁI KHOẢN VAY (Duyệt, kích hoạt, quá hạn...)
     * PATCH http://localhost:8080/api/loans/10/status?status=ACTIVE
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateLoanStatus(
            @PathVariable Long id,
            @RequestParam LoanStatus status
    ) {
        try {
            Loan result = loanService.updateLoanStatus(id, status);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}
