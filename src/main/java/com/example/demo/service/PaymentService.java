package com.example.demo.service;

import com.example.demo.entity.Employee;
import com.example.demo.entity.Payment;
import com.example.demo.entity.RepaymentSchedule;
import com.example.demo.entity.ScheduleStatus;
import com.example.demo.repository.PaymentRepository;
import com.example.demo.repository.RepaymentScheduleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private RepaymentScheduleRepository scheduleRepository;

    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }

    public Payment getPaymentById(Long id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("🔴 LỖI: Không tìm thấy giao dịch thanh toán có ID là " + id));
    }

    @Transactional
    public Payment processPayment(Payment newPayment) {
        // 1. Kiểm tra an toàn đầu vào
        if (newPayment.getRepaymentSchedule() == null || newPayment.getRepaymentSchedule().getScheduleId() == null) {
            throw new RuntimeException("🔴 LỖI: Hóa đơn thanh toán bắt buộc phải chỉ định mã kỳ hạn nợ!");
        }

        Long scheduleId = newPayment.getRepaymentSchedule().getScheduleId();

        // 2. Lấy thông tin kỳ hạn nợ gốc từ DB
        RepaymentSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new RuntimeException("🔴 LỖI: Không tìm thấy kỳ hạn trả nợ trên hệ thống!"));

        if (schedule.getStatus() == ScheduleStatus.PAID) {
            throw new RuntimeException("🔴 LỖI: Kỳ hạn này đã được thanh toán hoàn tất trước đó!");
        }

        // Tự động gán nhân viên nếu client không gửi lên
        if (newPayment.getEmployee() == null || newPayment.getEmployee().getEmployeeId() == null) {
            Employee loanManager = schedule.getLoan().getEmployee();
            if (loanManager == null) {
                throw new IllegalStateException("LỖI NGHIỆP VỤ: Khoản vay này chưa có nhân viên quản lý. Không thể thực hiện thanh toán.");
            }
            newPayment.setEmployee(loanManager);
        }

        newPayment.setPaymentDate(LocalDate.now());

        // 3. Tính số tiền cần thu
        BigDecimal requiredAmount = schedule.getPrincipalAmount()
                .add(schedule.getInterestAmount())
                .add(schedule.getPenaltyAmount());

        // 4. Lưu và ép đẩy dữ liệu xuống DB ngay lập tức để debug
        paymentRepository.save(newPayment);
        paymentRepository.flush(); // <-- THÊM DÒNG NÀY ĐỂ DEBUG

        // 5. Quét lại tất cả các khoản thanh toán cho kỳ hạn này để tính tổng
        List<Payment> allPaymentsForSchedule = paymentRepository.findByRepaymentScheduleScheduleId(scheduleId);
        BigDecimal totalPaidSum = allPaymentsForSchedule.stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 6. Kiểm tra và cập nhật trạng thái kỳ hạn
        if (totalPaidSum.compareTo(requiredAmount) >= 0) {
            schedule.setStatus(ScheduleStatus.PAID);
            scheduleRepository.save(schedule);
        }

        return newPayment;
    }
}
