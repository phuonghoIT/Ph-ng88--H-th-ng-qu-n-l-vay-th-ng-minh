package com.example.demo.controller;

import com.example.demo.entity.Branch;
import com.example.demo.service.BranchService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/branches")
@CrossOrigin(origins = "*") // Mở cửa CORS để Sơn và Việt gọi API không bị chặn
public class BranchController {

    @Autowired
    private BranchService branchService;

    /**
     * 🟢 1. LẤY DANH SÁCH TẤT CẢ CHI NHÁNH
     * GET http://localhost:8080/api/branches
     */
    @GetMapping
    public ResponseEntity<List<Branch>> getAllBranches() {
        List<Branch> branches = branchService.getAllBranches();

        // Thùng hàng thành công: Nhãn 200 OK + Ruột là danh sách chi nhánh
        return ResponseEntity.ok(branches);
    }

    /**
     * 🟢 2. XEM CHI TIẾT MỘT CHI NHÁNH THEO ID
     * GET http://localhost:8080/api/branches/5
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getBranchById(@PathVariable Long id) {
        try {
            Branch branch = branchService.getBranchById(id);
            return ResponseEntity.ok(branch);
        } catch (RuntimeException e) {
            // Nếu không tìm thấy ID, dán nhãn 400 Bad Request + câu báo lỗi
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    /**
     * 🔵 3. TẠO MỚI MỘT CHI NHÁNH (Dành cho Admin)
     * POST http://localhost:8080/api/branches
     * Body gửi lên: Cục JSON chứa thông tin chi nhánh
     */
    @PostMapping
    public ResponseEntity<?> createBranch(@Valid @RequestBody Branch newBranch) {
        try {
            Branch savedBranch = branchService.createBranch(newBranch);

            // Tạo mới thành công: Dán nhãn 201 CREATED + Ruột là chi nhánh mới đẻ
            return ResponseEntity.status(HttpStatus.CREATED).body(savedBranch);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    /**
     * 🟡 4. SỬA THÔNG TIN CHI NHÁNH (Dành cho Admin)
     * PUT http://localhost:8080/api/branches/5
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateBranch(
            @PathVariable Long id,
            @Valid @RequestBody Branch updatedData
    ) {
        try {
            Branch result = branchService.updateBranch(id, updatedData);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}