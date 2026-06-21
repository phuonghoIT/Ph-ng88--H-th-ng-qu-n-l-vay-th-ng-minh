package com.example.demo.controller;

import com.example.demo.entity.RepaymentSchedule;
import com.example.demo.service.RepaymentScheduleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/repayment-schedules")
@CrossOrigin(origins = "*")
public class RepaymentScheduleController {

    @Autowired
    private RepaymentScheduleService scheduleService;

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

    @GetMapping("/{id}")
    public ResponseEntity<?> getScheduleById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(scheduleService.getScheduleById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    /**
     * 🟢 3. API MỚI: XEM CHI TIẾT KỲ TRẢ NỢ (Bao gồm số tiền đã đóng)
     * GET http://localhost:8080/api/repayment-schedules/25/detail
     */
    @GetMapping("/{id}/detail")
    public ResponseEntity<?> getScheduleDetail(@PathVariable Long id) {
        try {
            Map<String, Object> details = scheduleService.getScheduleDetailsAsMap(id);
            return ResponseEntity.ok(details);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
}
