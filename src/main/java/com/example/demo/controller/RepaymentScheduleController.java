package com.example.demo.controller;

import com.example.demo.entity.RepaymentSchedule;
import com.example.demo.service.RepaymentScheduleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/repayment-schedules")
@CrossOrigin(origins = "*")
public class RepaymentScheduleController {

    @Autowired
    private RepaymentScheduleService scheduleService;

    /**
     * 🟢 1. LẤY LỊCH TRẢ NỢ THEO MÃ KHOẢN VAY
     * GET http://localhost:8080/api/repayment-schedules?loanId=10
     */
    @GetMapping
    public ResponseEntity<?> getSchedules(@RequestParam(required = false) Long loanId) {
        if (loanId == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("🔴 LỖI: Bắt buộc truyền tham số loanId để xem lịch trả nợ!");
        }
        try {
            List<RepaymentSchedule> schedules = scheduleService.getSchedulesByLoanId(loanId);
            return ResponseEntity.ok(schedules);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    /**
     * 🟢 2. XEM CHI TIẾT MỘT KỲ TRẢ NỢ THEO ID
     * GET http://localhost:8080/api/repayment-schedules/25
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getScheduleById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(scheduleService.getScheduleById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
}
