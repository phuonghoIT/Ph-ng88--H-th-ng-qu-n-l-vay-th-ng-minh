package com.example.demo.controller;

import com.example.demo.entity.Collateral;
import com.example.demo.service.CollateralService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/collaterals")
@CrossOrigin(origins = "*")
public class CollateralController {

    @Autowired
    private CollateralService collateralService;

    /**
     * 🟢 1. LẤY DANH SÁCH TÀI SẢN THẾ CHẤP
     * GET http://localhost:8080/api/collaterals
     */
    @GetMapping
    public ResponseEntity<List<Collateral>> getAllCollaterals() {
        return ResponseEntity.ok(collateralService.getAllCollaterals());
    }

    /**
     * 🟢 2. XEM CHI TIẾT TÀI SẢN THẾ CHẤP THEO ID
     * GET http://localhost:8080/api/collaterals/5
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getCollateralById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(collateralService.getCollateralById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    /**
     * 🔵 3. ĐĂNG KÝ TÀI SẢN THẾ CHẤP CHO KHOẢN VAY
     * POST http://localhost:8080/api/collaterals
     */
    @PostMapping
    public ResponseEntity<?> createCollateral(@Valid @RequestBody Collateral newCollateral) {
        try {
            Collateral saved = collateralService.createCollateral(newCollateral);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    /**
     * 🟡 4. CẬP NHẬT THÔNG TIN TÀI SẢN THẾ CHẤP
     * PUT http://localhost:8080/api/collaterals/5
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateCollateral(
            @PathVariable Long id,
            @Valid @RequestBody Collateral updatedData
    ) {
        try {
            return ResponseEntity.ok(collateralService.updateCollateral(id, updatedData));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}
