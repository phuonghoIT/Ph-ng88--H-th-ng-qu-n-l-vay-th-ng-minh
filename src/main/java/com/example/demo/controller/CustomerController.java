package com.example.demo.controller;

import com.example.demo.entity.Customers; // Khớp với tên Entity viết hoa của em
import com.example.demo.service.CustomerService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
@CrossOrigin(origins = "*") // Mở cửa cho Sơn và Việt kết nối Front-end thoải mái
public class CustomerController {

    @Autowired
    private CustomerService customerService;

    /**
     * 🟢 1. LẤY DANH SÁCH TẤT CẢ KHÁCH HÀNG
     * GET http://localhost:8080/api/customers
     */
    @GetMapping
    public ResponseEntity<List<Customers>> getAllCustomers() {
        List<Customers> list = customerService.getAllCustomers();
        return ResponseEntity.ok(list);
    }

    /**
     * 🟢 2. XEM CHI TIẾT HỒ SƠ 1 KHÁCH HÀNG THEO ID
     * GET http://localhost:8080/api/customers/5
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getCustomerById(@PathVariable Long id) {
        try {
            Customers customer = customerService.getCustomerById(id);
            return ResponseEntity.ok(customer);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    /**
     * 🔵 3. ĐĂNG KÝ KHÁCH HÀNG MỚI (Mở tài khoản)
     * POST http://localhost:8080/api/customers
     */
    @PostMapping
    public ResponseEntity<?> createCustomer(@Valid @RequestBody Customers newCustomer) {
        try {
            Customers saved = customerService.createCustomer(newCustomer);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    /**
     * 🟡 4. CẬP NHẬT THÔNG TIN CÁ NHÂN KHÁCH HÀNG
     * PUT http://localhost:8080/api/customers/5
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateCustomer(
            @PathVariable Long id,
            @Valid @RequestBody Customers updatedData
    ) {
        try {
            Customers result = customerService.updateCustomer(id, updatedData);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    /**
     * 🟢 5. API TÌM KIẾM KHÁCH HÀNG BẰNG CCCD
     * GET http://localhost:8080/api/customers/search?cccd=03710600xxxx
     */
    @GetMapping("/search")
    public ResponseEntity<?> getCustomerByIdentityNumber(@RequestParam("cccd") String cccd) {
        try {
            Customers customer = customerService.getCustomerByIdentityNumber(cccd);
            // Tìm thấy thì dán nhãn 200 OK và nhét hồ sơ khách vào ruột thùng hàng
            return ResponseEntity.ok(customer);
        } catch (RuntimeException e) {
            // Không thấy thì dán nhãn 400 Bad Request kèm câu thông báo lỗi cho Front-end
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}