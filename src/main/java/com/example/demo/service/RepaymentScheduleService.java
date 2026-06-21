package com.example.demo.service;

import com.example.demo.entity.Payment;
import com.example.demo.entity.RepaymentSchedule;
import com.example.demo.repository.PaymentRepository;
import com.example.demo.repository.RepaymentScheduleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RepaymentScheduleService {

    @Autowired
    private RepaymentScheduleRepository scheduleRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    public RepaymentSchedule getScheduleById(Long id) {
        return scheduleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("🔴 LỖI: Không tìm thấy kỳ trả nợ có ID là " + id));
    }

    public List<RepaymentSchedule> getSchedulesByLoanId(Long loanId) {
        return scheduleRepository.findByLoanLoanIdOrderByPeriodNumberAsc(loanId);
    }

    public Map<String, Object> getScheduleDetailsAsMap(Long scheduleId) {
        RepaymentSchedule schedule = getScheduleById(scheduleId);

        // Lấy tất cả các khoản thanh toán cho kỳ hạn này
        List<Payment> payments = paymentRepository.findByRepaymentScheduleScheduleId(scheduleId);
        
        // Tính tổng số tiền đã đóng
        BigDecimal totalPaid = payments.stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Tính tổng số tiền cần phải đóng
        BigDecimal requiredAmount = schedule.getPrincipalAmount()
                .add(schedule.getInterestAmount())
                .add(schedule.getPenaltyAmount());

        // Đóng gói dữ liệu vào Map để trả về
        Map<String, Object> details = new HashMap<>();
        details.put("scheduleId", schedule.getScheduleId());
        details.put("periodNumber", schedule.getPeriodNumber());
        details.put("dueDate", schedule.getDueDate());
        details.put("status", schedule.getStatus().toString());
        details.put("principalAmount", schedule.getPrincipalAmount());
        details.put("interestAmount", schedule.getInterestAmount());
        details.put("penaltyAmount", schedule.getPenaltyAmount());
        details.put("requiredAmount", requiredAmount); // Tổng tiền phải trả
        details.put("totalPaid", totalPaid);         // Tổng tiền đã trả

        return details;
    }
}
